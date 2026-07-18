package com.alongside.core.domain.diary.processing

/**
 * Vision-based episode description seam. Takes raw image bytes rather than
 * [com.alongside.core.model.diary.Photo]/URIs - reading bytes off a photo URI is a platform
 * concern resolved in feature:diary, not here. Real implementation (Gemini) lives in core:network.
 */
public interface EpisodeVisionDescriptionClient {
    public suspend fun describeEpisode(
        images: List<ByteArray>,
        placeName: String?,
    ): VisionDescriptionResult
}
