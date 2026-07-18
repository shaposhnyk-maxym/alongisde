package com.alongside.data.sync

import com.alongside.core.database.sync.PersistedSyncOperation
import com.alongside.core.database.sync.PersistedSyncOperationStatus
import com.alongside.core.database.sync.SyncOperationStore

/** List-backed [SyncOperationStore] fake mirroring the Room implementation's semantics. */
internal class InMemorySyncOperationStore : SyncOperationStore {
    private val records = mutableListOf<PersistedSyncOperation>()

    override suspend fun append(record: PersistedSyncOperation) {
        records += record
    }

    override suspend fun loadAll(): List<PersistedSyncOperation> = records.toList()

    override suspend fun remove(ids: List<String>) {
        records.removeAll { it.id in ids }
    }

    override suspend fun markRetry(
        id: String,
        attempts: Int,
    ) {
        val index = records.indexOfFirst { it.id == id }
        if (index >= 0) {
            records[index] =
                records[index].copy(status = PersistedSyncOperationStatus.RETRY, attempts = attempts)
        }
    }
}
