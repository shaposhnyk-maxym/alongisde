package com.alongside.core.network.gemini

import com.alongside.core.domain.diary.processing.EpisodeVisionDescriptionClient
import com.alongside.core.domain.diary.processing.VisionDescriptionResult
import com.alongside.core.network.gemini.model.Content
import com.alongside.core.network.gemini.model.GenerateContentRequest
import com.alongside.core.network.gemini.model.InlineData
import com.alongside.core.network.gemini.model.Part
import com.alongside.core.network.gemini.model.firstText
import kotlinx.coroutines.CancellationException
import kotlin.io.encoding.Base64

/** Adapts [GeminiVisionApi] to the [EpisodeVisionDescriptionClient] seam core:domain depends on. */
public class GeminiVisionDescriptionClient(
    private val api: GeminiVisionApi,
) : EpisodeVisionDescriptionClient {
    override suspend fun describeEpisode(
        images: List<ByteArray>,
        placeName: String?,
    ): VisionDescriptionResult =
        try {
            val response = api.generateContent(buildRequest(images, placeName))
            val text = response.firstText()
            if (text != null) {
                VisionDescriptionResult.Generated(text)
            } else {
                val cause = GeminiException.MalformedResponse("Gemini response had no text content")
                VisionDescriptionResult.Failure(cause)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: GeminiException) {
            VisionDescriptionResult.Failure(e)
        }

    private fun buildRequest(
        images: List<ByteArray>,
        placeName: String?,
    ): GenerateContentRequest {
        val imageParts = images.map { Part(inlineData = InlineData(mimeType = "image/jpeg", data = Base64.encode(it))) }
        val parts = listOf(Part(text = describePrompt(placeName))) + imageParts
        return GenerateContentRequest(contents = listOf(Content(parts = parts)))
    }

    // Concept doc: "emotional paragraph with impressions, not a dry template."
    private fun describePrompt(placeName: String?): String {
        val locationHint = placeName?.let { " at $it" } ?: ""
        return "You are writing a personal travel diary entry$locationHint based on these photos, taken by " +
            "someone travelling with their partner. Write one short, warm, emotional paragraph (2-4 sentences) " +
            "capturing the feeling and impressions of this moment - not a dry factual description of what's " +
            "in the photos."
    }
}
