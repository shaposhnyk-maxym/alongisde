package com.alongside.core.network.gemini.model

import kotlinx.serialization.Serializable

@Serializable
public data class GenerateContentResponse(
    val candidates: List<Candidate> = emptyList(),
)

@Serializable
public data class Candidate(
    val content: Content? = null,
    val finishReason: String? = null,
)

/** First text part across the first candidate - Gemini can return multiple parts/candidates, we want one string. */
public fun GenerateContentResponse.firstText(): String? {
    val parts = candidates.firstOrNull()?.content?.parts ?: return null
    return parts.firstNotNullOfOrNull { it.text }
}
