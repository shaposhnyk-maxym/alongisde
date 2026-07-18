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
        languageTag: String,
    ): VisionDescriptionResult =
        try {
            val response = api.generateContent(buildRequest(images, placeName, languageTag))
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
        languageTag: String,
    ): GenerateContentRequest {
        val imageParts = images.map { Part(inlineData = InlineData(mimeType = "image/jpeg", data = Base64.encode(it))) }
        val parts = listOf(Part(text = describePrompt(placeName, languageTag))) + imageParts
        return GenerateContentRequest(contents = listOf(Content(parts = parts)))
    }

    // Verified live against real photos (M10 manual test) in both "uk" and "en" before landing -
    // the anti-cliche/concrete-detail constraints measurably changed output from generic
    // travel-blog prose ("wrapped up in the warmth... a dream we never want to wake up from") to
    // specific, grounded captions ("kick through the dry brown leaves on the gravel").
    private fun describePrompt(
        placeName: String?,
        languageTag: String,
    ): String {
        val locationClause = placeName?.let { " at $it" } ?: ""
        return "Write one diary caption in the language with BCP-47 tag '$languageTag' for a couple " +
            "documenting a trip together, based only on what's actually visible in these photos" +
            "$locationClause.\n\n" +
            "Requirements:\n" +
            "- Native $languageTag, not a translation - natural idiom, contractions and rhythm a real " +
            "native speaker would use.\n" +
            "- 1-2 sentences. This is a caption, not an essay.\n" +
            "- Anchor it in one concrete, specific detail actually visible in the photos (an object, a " +
            "color, a texture, light) - never a vague mood word.\n" +
            "- Sound like a real person's private diary note, not a travel blog, postcard, or ad copy. " +
            "Avoid: \"magical\", \"breathtaking\", \"unforgettable\", \"wrapped up in\", \"never wanted " +
            "it to end\", exclamation marks, emoji.\n" +
            "- First person plural (\"we\"), casual, specific to this exact moment - not generic " +
            "scene-setting.\n\n" +
            "Output only the caption itself, nothing else - no quotes, no labels."
    }
}
