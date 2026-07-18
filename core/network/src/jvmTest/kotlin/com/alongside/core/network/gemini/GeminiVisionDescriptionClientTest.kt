package com.alongside.core.network.gemini

import com.alongside.core.domain.diary.processing.VisionDescriptionResult
import io.ktor.content.TextContent
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

// runBlocking, not runTest: see the M3 note in docs/roadmap.md.
class GeminiVisionDescriptionClientTest {
    @Test
    fun `successful response maps to Generated with the candidate text`() =
        runBlocking {
            val api =
                testGeminiVisionApi {
                    respondJson(
                        """{"candidates": [{"content": {"parts": [{"text": "A wander through the old town."}]}}]}""",
                    )
                }
            val client = GeminiVisionDescriptionClient(api)

            val result = client.describeEpisode(listOf(byteArrayOf(1, 2, 3)), placeName = "Rynok Square")

            assertEquals(VisionDescriptionResult.Generated("A wander through the old town."), result)
        }

    @Test
    fun `sends one inline-data image part per input image plus the prompt text part`() =
        runBlocking {
            var capturedBody: String? = null
            val api =
                testGeminiVisionApi { req ->
                    capturedBody = (req.body as TextContent).text
                    respondJson("""{"candidates": [{"content": {"parts": [{"text": "ok"}]}}]}""")
                }
            val client = GeminiVisionDescriptionClient(api)

            client.describeEpisode(listOf(byteArrayOf(1), byteArrayOf(2), byteArrayOf(3)), placeName = "Rynok Square")

            val body = capturedBody!!
            assertTrue(body.contains("Rynok Square"))
            assertEquals(3, Regex("inline_data").findAll(body).count())
        }

    @Test
    fun `response with no text content maps to Failure`() {
        runBlocking {
            val api = testGeminiVisionApi { respondJson("""{"candidates": []}""") }
            val client = GeminiVisionDescriptionClient(api)

            val result = client.describeEpisode(listOf(byteArrayOf(1)), placeName = null)

            assertIs<VisionDescriptionResult.Failure>(result)
        }
    }

    @Test
    fun `HTTP-level failure maps to Failure, not an exception`() {
        runBlocking {
            val api =
                testGeminiVisionApi {
                    respondJson("""{"error": {"message": "overloaded"}}""", HttpStatusCode.ServiceUnavailable)
                }
            val client = GeminiVisionDescriptionClient(api)

            val result = client.describeEpisode(listOf(byteArrayOf(1)), placeName = null)

            assertIs<VisionDescriptionResult.Failure>(result)
        }
    }
}
