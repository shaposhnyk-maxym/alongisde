package com.alongside.core.network.firestore.model

import kotlinx.serialization.Serializable

/** Google's standard API error envelope, returned by Firestore REST on non-2xx responses. */
@Serializable
public data class FirestoreErrorResponse(
    public val error: FirestoreErrorDetail,
)

@Serializable
public data class FirestoreErrorDetail(
    public val code: Int,
    public val message: String,
    public val status: String,
)
