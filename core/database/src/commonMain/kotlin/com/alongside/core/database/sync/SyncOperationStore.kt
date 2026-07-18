package com.alongside.core.database.sync

import kotlin.time.Instant

public enum class PersistedSyncOperationType { UPSERT, DELETE }

public enum class PersistedSyncOperationStatus { PENDING, RETRY }

/**
 * A sync operation as persisted between app runs. Deliberately network-agnostic:
 * core:database cannot see core:network's `SyncOperation`/`FirestoreValue`, so the
 * document fields travel as an opaque [fieldsJson] blob the `data` module encodes/decodes.
 */
public data class PersistedSyncOperation(
    public val id: String,
    public val collectionPath: String,
    public val documentId: String,
    public val type: PersistedSyncOperationType,
    public val fieldsJson: String,
    public val attempts: Int,
    public val status: PersistedSyncOperationStatus,
    public val enqueuedAt: Instant,
)

/**
 * Durable FIFO of pending sync operations - the persistent counterpart of
 * core:network's in-memory `SyncQueue`, owned by the `data` module's coordinator.
 */
public interface SyncOperationStore {
    public suspend fun append(record: PersistedSyncOperation)

    /** Every stored operation ([PersistedSyncOperationStatus.PENDING] and RETRY alike), FIFO by insertion order. */
    public suspend fun loadAll(): List<PersistedSyncOperation>

    public suspend fun remove(ids: List<String>)

    /** Keeps the operation in the store, marked [PersistedSyncOperationStatus.RETRY] with its attempt count. */
    public suspend fun markRetry(
        id: String,
        attempts: Int,
    )
}
