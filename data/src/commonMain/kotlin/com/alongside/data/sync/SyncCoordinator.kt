package com.alongside.data.sync

import com.alongside.core.database.sync.SyncOperationStore
import com.alongside.core.model.SyncStatus
import com.alongside.core.network.firestore.FirestoreException
import com.alongside.core.network.firestore.model.FirestoreDocument
import com.alongside.core.network.firestore.model.FirestoreValue
import com.alongside.core.network.queue.InMemorySyncQueue
import com.alongside.core.network.queue.SyncBatchResult
import com.alongside.core.network.queue.SyncOperation
import com.alongside.core.network.queue.SyncOperationType
import com.alongside.core.network.queue.SyncQueueProcessor
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Instant

/**
 * Drives one offline-first sync round: loads the durable queue, resolves last-write-wins
 * conflicts against the remote (preflight read per UPSERT), pushes the surviving operations
 * through the untouched [SyncQueueProcessor], then applies the outcome - succeeded operations
 * leave the store and mark their rows SYNCED, failed ones stay marked RETRY with their attempt
 * count and mark their rows FAILED, so the next [sync] retries them.
 */
public class SyncCoordinator(
    private val store: SyncOperationStore,
    private val processor: SyncQueueProcessor,
    private val remoteReader: RemoteDocumentReader,
    private val bindings: List<SyncEntityBinding>,
) {
    private val mutex = Mutex()

    public suspend fun sync(): SyncBatchResult =
        mutex.withLock {
            val records = store.loadAll()
            val toPush = mutableListOf<SyncOperation>()
            for (record in records) {
                val operation = SyncOperationCodec.toOperation(record)
                when (preflight(operation)) {
                    PreflightOutcome.PUSH -> toPush += operation
                    PreflightOutcome.REMOTE_WON -> store.remove(listOf(record.id))
                    PreflightOutcome.UNREACHABLE -> store.markRetry(record.id, record.attempts)
                }
            }
            val result = processor.processAll(InMemorySyncQueue().apply { enqueueAll(toPush) })
            applyBatchResult(result)
            result
        }

    private enum class PreflightOutcome { PUSH, REMOTE_WON, UNREACHABLE }

    // Any preflight-read failure (offline, permission-denied, etc.) is treated the same way:
    // retry later rather than surface the specific cause here.
    @Suppress("SwallowedException")
    private suspend fun preflight(operation: SyncOperation): PreflightOutcome {
        val localUpdatedAt = operation.fields.updatedAtOrNull()
        if (operation.type != SyncOperationType.UPSERT || localUpdatedAt == null) {
            return PreflightOutcome.PUSH
        }
        return try {
            val remote = remoteReader.read(operation.collectionPath, operation.documentId)
            when (resolveConflict(localUpdatedAt, remote?.fields?.updatedAtOrNull())) {
                ConflictWinner.LOCAL -> PreflightOutcome.PUSH
                ConflictWinner.REMOTE -> {
                    applyRemote(operation.collectionPath, checkNotNull(remote))
                    PreflightOutcome.REMOTE_WON
                }
            }
        } catch (e: FirestoreException) {
            PreflightOutcome.UNREACHABLE
        }
    }

    // markStatus runs BEFORE the operation leaves the durable queue, not after: if this
    // coroutine is cancelled between the two (e.g. the screen observing this sync loop leaves
    // composition right after a push completes), a push that already succeeded but never got
    // this far would otherwise be gone from the queue forever with no way to retry it, while the
    // local row stays stuck at its pre-sync status permanently. Reversed, the worst case of the
    // same cancellation is a harmless redundant re-push next cycle - store.remove() is what's
    // left undone.
    private suspend fun applyBatchResult(result: SyncBatchResult) {
        if (result.succeeded.isNotEmpty()) {
            for (operation in result.succeeded) {
                markStatus(operation, SyncStatus.SYNCED)
            }
            store.remove(result.succeeded.map { it.id })
        }
        for (operation in result.failed) {
            markStatus(operation, SyncStatus.FAILED)
            store.markRetry(operation.id, operation.attempts)
        }
    }

    private suspend fun applyRemote(
        collectionPath: String,
        document: FirestoreDocument,
    ) {
        bindingFor(collectionPath)?.applyRemote(document)
    }

    private suspend fun markStatus(
        operation: SyncOperation,
        status: SyncStatus,
    ) {
        val binding = bindingFor(operation.collectionPath)
        if (binding == null) {
            println(
                "SyncCoordinator: NO BINDING for '${operation.collectionPath}' - known bindings=" +
                    bindings.map { it.collectionPath },
            )
        }
        binding?.markStatus(operation.documentId, status)
    }

    private fun bindingFor(path: String): SyncEntityBinding? = bindings.find { it.collectionPath == path }

    private fun Map<String, FirestoreValue>.updatedAtOrNull(): Instant? =
        (this["updatedAt"] as? FirestoreValue.TimestampValue)?.let { timestamp ->
            try {
                Instant.parse(timestamp.value)
            } catch (_: IllegalArgumentException) {
                null
            }
        }
}
