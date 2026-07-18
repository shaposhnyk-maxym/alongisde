package com.alongside.core.network.gemini.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class Part(
    val text: String? = null,
    @SerialName("inline_data") val inlineData: InlineData? = null,
)

@Serializable
public data class InlineData(
    @SerialName("mime_type") val mimeType: String,
    val data: String,
)
