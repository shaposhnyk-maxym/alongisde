package com.alongside.data.place

import com.alongside.core.domain.place.PlaceCandidateRepository
import com.alongside.core.model.SyncStatus
import com.alongside.core.network.firestore.model.FirestoreDocument
import com.alongside.data.sync.SyncEntityBinding

/**
 * Writes go through the plain local repository, never [SyncingPlaceCandidateRepository] - status
 * flips and remote applications must not re-enqueue sync work or touch `updatedAt`.
 */
public class PlaceCandidateSyncEntityBinding(
    private val local: PlaceCandidateRepository,
) : SyncEntityBinding {
    override val collectionPath: String = PlaceCandidateFirestoreMapper.COLLECTION_PATH

    override suspend fun applyRemote(document: FirestoreDocument) {
        local.upsert(PlaceCandidateFirestoreMapper.fromDocument(document))
    }

    override suspend fun markStatus(
        documentId: String,
        status: SyncStatus,
    ) {
        val existing = local.getById(documentId) ?: return
        local.upsert(existing.copy(syncStatus = status))
    }
}
