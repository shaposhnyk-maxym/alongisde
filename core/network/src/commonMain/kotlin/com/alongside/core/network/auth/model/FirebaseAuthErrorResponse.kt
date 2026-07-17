package com.alongside.core.network.auth.model

import kotlinx.serialization.Serializable

/** Google's standard API error envelope, returned by Identity Toolkit REST on non-2xx responses. */
@Serializable
public data class FirebaseAuthErrorResponse(
    public val error: FirebaseAuthErrorDetail,
)

@Serializable
public data class FirebaseAuthErrorDetail(
    public val code: Int,
    public val message: String,
    public val status: String,
)
