package com.alongside.core.database.repository

import com.alongside.core.database.AlongsideDatabase
import com.alongside.core.database.entity.toEntity
import com.alongside.core.database.entity.toRecord
import com.alongside.core.database.sync.PersistedSyncOperation
import com.alongside.core.database.sync.PersistedSyncOperationStatus
import com.alongside.core.database.sync.SyncOperationStore

internal class SyncOperationStoreImpl(
    database: AlongsideDatabase,
) : SyncOperationStore {
    private val dao = database.syncOperationDao()

    override suspend fun append(record: PersistedSyncOperation) {
        dao.insert(record.toEntity())
    }

    override suspend fun loadAll(): List<PersistedSyncOperation> = dao.getAll().map { it.toRecord() }

    override suspend fun remove(ids: List<String>) {
        dao.deleteByOpIds(ids)
    }

    override suspend fun markRetry(
        id: String,
        attempts: Int,
    ) {
        dao.updateStatus(id, PersistedSyncOperationStatus.RETRY.name, attempts)
    }
}
