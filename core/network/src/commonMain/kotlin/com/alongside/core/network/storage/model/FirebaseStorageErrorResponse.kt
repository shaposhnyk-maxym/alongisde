package com.alongside.core.network.storage.model

import kotlinx.serialization.Serializable

/** Firebase Storage's error body follows the standard Google API error envelope. */
@Serializable
public data class FirebaseStorageErrorResponse(
    public val error: FirebaseStorageErrorDetail,
)

@Serializable
public data class FirebaseStorageErrorDetail(
    public val code: Int,
    public val message: String,
    public val status: String? = null,
)
