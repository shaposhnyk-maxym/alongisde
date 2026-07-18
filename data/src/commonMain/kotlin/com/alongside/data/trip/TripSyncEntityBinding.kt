package com.alongside.data.trip

import com.alongside.core.domain.trip.TripRepository
import com.alongside.core.model.SyncStatus
import com.alongside.core.network.firestore.model.FirestoreDocument
import com.alongside.data.sync.SyncEntityBinding

/**
 * Writes go through the plain local repository, never [SyncingTripRepository] - status
 * flips and remote applications must not re-enqueue sync work or touch `updatedAt`.
 */
public class TripSyncEntityBinding(
    private val local: TripRepository,
) : SyncEntityBinding {
    override val collectionPath: String = TripFirestoreMapper.COLLECTION_PATH

    override suspend fun applyRemote(document: FirestoreDocument) {
        local.upsert(TripFirestoreMapper.fromDocument(document))
    }

    override suspend fun markStatus(
        documentId: String,
        status: SyncStatus,
    ) {
        val existing = local.getById(documentId) ?: return
        local.upsert(existing.copy(syncStatus = status))
    }
}
