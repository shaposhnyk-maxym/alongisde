package com.alongside.data.sync

import com.alongside.core.network.firestore.FirestoreException
import com.alongside.core.network.firestore.model.FirestoreDocument

/** Scripted remote for preflight reads: per-document documents, or a blanket outage. */
internal class FakeRemoteDocumentReader : RemoteDocumentReader {
    val documents = mutableMapOf<String, FirestoreDocument>()
    var unreachable: Boolean = false
    val readDocumentIds = mutableListOf<String>()

    override suspend fun read(
        collectionPath: String,
        documentId: String,
    ): FirestoreDocument? {
        readDocumentIds += documentId
        if (unreachable) {
            throw FirestoreException.ServerError(code = 503, status = "UNAVAILABLE", message = "scripted outage")
        }
        return documents[documentId]
    }
}
