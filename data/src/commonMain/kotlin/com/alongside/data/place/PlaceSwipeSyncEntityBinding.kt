package com.alongside.data.place

import com.alongside.core.domain.place.PlaceSwipeRepository
import com.alongside.core.model.SyncStatus
import com.alongside.core.network.firestore.model.FirestoreDocument
import com.alongside.data.sync.SyncEntityBinding

/**
 * Writes go through the plain local repository, never [SyncingPlaceSwipeRepository] - status
 * flips and remote applications must not re-enqueue sync work or touch `updatedAt`. Mirrors
 * [PlaceCandidateSyncEntityBinding] exactly.
 */
public class PlaceSwipeSyncEntityBinding(
    private val local: PlaceSwipeRepository,
) : SyncEntityBinding {
    override val collectionPath: String = PlaceSwipeFirestoreMapper.COLLECTION_PATH

    override suspend fun applyRemote(document: FirestoreDocument) {
        local.upsert(PlaceSwipeFirestoreMapper.fromDocument(document))
    }

    override suspend fun markStatus(
        documentId: String,
        status: SyncStatus,
    ) {
        val existing = local.getById(documentId) ?: return
        local.upsert(existing.copy(syncStatus = status))
    }
}
