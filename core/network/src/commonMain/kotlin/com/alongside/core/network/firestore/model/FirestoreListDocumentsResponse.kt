package com.alongside.core.network.firestore.model

import kotlinx.serialization.Serializable

/** See: https://firebase.google.com/docs/firestore/reference/rest/v1/projects.databases.documents/list */
@Serializable
public data class FirestoreListDocumentsResponse(
    public val documents: List<FirestoreDocument> = emptyList(),
    public val nextPageToken: String? = null,
)
