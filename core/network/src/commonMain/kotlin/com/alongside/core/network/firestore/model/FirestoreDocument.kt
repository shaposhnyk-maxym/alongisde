package com.alongside.core.network.firestore.model

import kotlinx.serialization.Serializable

/** See: https://firebase.google.com/docs/firestore/reference/rest/v1/projects.databases.documents#Document */
@Serializable
public data class FirestoreDocument(
    public val name: String? = null,
    public val fields: Map<String, FirestoreValue> = emptyMap(),
    public val createTime: String? = null,
    public val updateTime: String? = null,
)

/** Request body for creating/overwriting a document - `name`/timestamps are server-assigned. */
@Serializable
public data class FirestoreWriteRequest(
    public val fields: Map<String, FirestoreValue> = emptyMap(),
)
