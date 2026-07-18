package com.alongside.core.domain.diary.processing

/**
 * Vision-based episode description seam. Takes raw image bytes rather than
 * [com.alongside.core.model.diary.Photo]/URIs - reading bytes off a photo URI is a platform
 * concern resolved in feature:diary, not here. Real implementation (Gemini) lives in core:network.
 *
 * [languageTag] is a BCP-47 tag (e.g. "uk", "en") for the app's current locale at capture time -
 * passed per call rather than baked into the client at construction, so a locale change takes
 * effect on the next capture without needing to recreate the (singleton) client.
 */
public interface EpisodeVisionDescriptionClient {
    public suspend fun describeEpisode(
        images: List<ByteArray>,
        placeName: String?,
        languageTag: String,
    ): VisionDescriptionResult
}
