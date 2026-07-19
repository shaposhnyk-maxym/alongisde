package com.alongside.data.diary

import com.alongside.core.domain.diary.DiaryEntryRepository
import com.alongside.core.model.SyncStatus
import com.alongside.core.network.firestore.model.FirestoreDocument
import com.alongside.data.sync.SyncEntityBinding

/**
 * Writes go through the plain local repository, never [SyncingDiaryEntryRepository] - status
 * flips and remote applications must not re-enqueue sync work or touch `updatedAt`.
 */
public class DiaryEntrySyncEntityBinding(
    private val local: DiaryEntryRepository,
) : SyncEntityBinding {
    override val collectionPath: String = DiaryEntryFirestoreMapper.COLLECTION_PATH

    override suspend fun applyRemote(document: FirestoreDocument) {
        local.upsert(DiaryEntryFirestoreMapper.fromDocument(document))
    }

    override suspend fun markStatus(
        documentId: String,
        status: SyncStatus,
    ) {
        val existing = local.getById(documentId) ?: return
        local.upsert(existing.copy(syncStatus = status))
    }
}
