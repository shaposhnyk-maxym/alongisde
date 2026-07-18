package com.alongside.core.network.gemini.model

import kotlinx.serialization.Serializable

@Serializable
public data class Content(
    val parts: List<Part>,
)
