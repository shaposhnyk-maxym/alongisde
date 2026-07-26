package com.alongside.data.pretrip

import com.alongside.core.domain.pretrip.PreTripPhotoRepository
import com.alongside.core.model.SyncStatus
import com.alongside.core.network.firestore.model.FirestoreDocument
import com.alongside.data.sync.SyncEntityBinding

/**
 * Writes go through the plain local repository, never a syncing decorator - status flips and
 * remote applications must not re-enqueue sync work. Mirrors [com.alongside.data.place.PlaceCandidateSyncEntityBinding]
 * exactly.
 */
public class PreTripPhotoSyncEntityBinding(
    private val local: PreTripPhotoRepository,
) : SyncEntityBinding {
    override val collectionPath: String = PreTripPhotoFirestoreMapper.COLLECTION_PATH

    override suspend fun applyRemote(document: FirestoreDocument) {
        local.upsert(PreTripPhotoFirestoreMapper.fromDocument(document))
    }

    override suspend fun markStatus(
        documentId: String,
        status: SyncStatus,
    ) {
        val existing = local.getById(documentId) ?: return
        local.upsert(existing.copy(syncStatus = status))
    }
}
