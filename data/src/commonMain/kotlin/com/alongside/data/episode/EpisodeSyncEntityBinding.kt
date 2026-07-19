package com.alongside.data.episode

import com.alongside.core.domain.diary.EpisodeRepository
import com.alongside.core.model.SyncStatus
import com.alongside.core.network.firestore.model.FirestoreDocument
import com.alongside.data.sync.SyncEntityBinding

/**
 * Writes go through the plain local repository, never [SyncingEpisodeRepository] - status
 * flips and remote applications must not re-enqueue sync work or touch `updatedAt`.
 */
public class EpisodeSyncEntityBinding(
    private val local: EpisodeRepository,
) : SyncEntityBinding {
    override val collectionPath: String = EpisodeFirestoreMapper.COLLECTION_PATH

    override suspend fun applyRemote(document: FirestoreDocument) {
        local.upsert(EpisodeFirestoreMapper.fromDocument(document))
    }

    override suspend fun markStatus(
        documentId: String,
        status: SyncStatus,
    ) {
        val existing = local.getById(documentId) ?: return
        local.upsert(existing.copy(syncStatus = status))
    }
}
