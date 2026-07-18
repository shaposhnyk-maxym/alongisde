package com.alongside.data.sync

import com.alongside.core.model.SyncStatus
import com.alongside.core.network.firestore.model.FirestoreDocument

/**
 * Per-collection glue the [SyncCoordinator] uses to touch local storage without knowing
 * entity types: applying a winning remote document and flipping a row's [SyncStatus].
 */
public interface SyncEntityBinding {
    public val collectionPath: String

    /** The remote copy won last-write-wins - store it locally as [SyncStatus.SYNCED]. */
    public suspend fun applyRemote(document: FirestoreDocument)

    /** No-op when the row no longer exists locally (e.g. a completed DELETE). */
    public suspend fun markStatus(
        documentId: String,
        status: SyncStatus,
    )
}
