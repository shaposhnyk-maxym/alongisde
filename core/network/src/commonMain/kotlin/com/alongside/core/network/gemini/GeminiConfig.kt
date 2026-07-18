package com.alongside.core.network.gemini

/**
 * [modelId] defaults to the `gemini-flash-latest` alias (currently resolves to `gemini-3.5-flash`
 * - verified against the real API, M10 manual test) rather than pinning a specific model version:
 * `gemini-2.0-flash` (the original default) turned out to have zero free-tier quota
 * (`generate_content_free_tier_requests` limit: 0) by the time this was tested for real, even on
 * a fresh Free Tier project - Google rotates which specific model versions carry free-tier quota,
 * and an alias tracks that instead of going stale. Override for a specific pinned model.
 */
public class GeminiConfig(
    public val apiKey: String,
    public val modelId: String = "gemini-flash-latest",
) {
    public val generateContentUrl: String
        get() = "https://generativelanguage.googleapis.com/v1beta/models/$modelId:generateContent?key=$apiKey"
}
