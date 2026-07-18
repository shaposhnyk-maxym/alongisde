package com.alongside.core.network.gemini.model

import kotlinx.serialization.Serializable

/** Google's standard API error envelope, returned by the Generative Language API on non-2xx responses. */
@Serializable
public data class GeminiErrorResponse(
    public val error: GeminiErrorDetail,
)

@Serializable
public data class GeminiErrorDetail(
    public val code: Int,
    public val message: String,
    public val status: String,
)
