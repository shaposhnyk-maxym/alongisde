package com.alongside.data.sync

import com.alongside.core.network.firestore.FirestoreApi
import com.alongside.core.network.firestore.FirestoreException
import com.alongside.core.network.firestore.model.FirestoreDocument

/**
 * Preflight read for last-write-wins: null means "no remote document"; any other problem
 * surfaces as [FirestoreException] and leaves the operation queued for retry.
 */
public fun interface RemoteDocumentReader {
    public suspend fun read(
        collectionPath: String,
        documentId: String,
    ): FirestoreDocument?
}

public class FirestoreRemoteDocumentReader(
    private val api: FirestoreApi,
) : RemoteDocumentReader {
    override suspend fun read(
        collectionPath: String,
        documentId: String,
    ): FirestoreDocument? =
        try {
            api.getDocument(collectionPath, documentId)
        } catch (e: FirestoreException.ClientError) {
            if (e.code == NOT_FOUND) null else throw e
        }

    private companion object {
        const val NOT_FOUND = 404
    }
}
