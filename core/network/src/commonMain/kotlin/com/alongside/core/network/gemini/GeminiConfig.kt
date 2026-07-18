package com.alongside.core.network.gemini

/** [modelId] defaults to a free-tier-eligible flash model; override for a different Gemini model. */
public class GeminiConfig(
    public val apiKey: String,
    public val modelId: String = "gemini-2.0-flash",
) {
    public val generateContentUrl: String
        get() = "https://generativelanguage.googleapis.com/v1beta/models/$modelId:generateContent?key=$apiKey"
}
