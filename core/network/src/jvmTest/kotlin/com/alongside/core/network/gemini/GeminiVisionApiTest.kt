package com.alongside.core.network.gemini

import com.alongside.core.network.gemini.model.Content
import com.alongside.core.network.gemini.model.GenerateContentRequest
import com.alongside.core.network.gemini.model.Part
import io.ktor.content.TextContent
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

// runBlocking, not runTest: runTest's virtual-time scheduler falsely times out
// Ktor HttpTimeout against MockEngine (see the M3 note in docs/roadmap.md).
class GeminiVisionApiTest {
    private val okResponseJson =
        """
        {
          "candidates": [
            {
              "content": {"parts": [{"text": "A warm afternoon wandering the old town together."}]},
              "finishReason": "STOP"
            }
          ]
        }
        """.trimIndent()

    private val request = GenerateContentRequest(contents = listOf(Content(parts = listOf(Part(text = "describe")))))

    @Test
    fun `generateContent posts JSON to the model endpoint with the api key`() =
        runBlocking {
            var capturedUrl: String? = null
            var capturedMethod: HttpMethod? = null
            var capturedContentType: String? = null
            val api =
                testGeminiVisionApi { req ->
                    capturedUrl = req.url.toString()
                    capturedMethod = req.method
                    capturedContentType = req.body.contentType.toString()
                    respondJson(okResponseJson)
                }

            api.generateContent(request)

            val expectedUrl =
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent" +
                    "?key=test-api-key"
            assertEquals(expectedUrl, capturedUrl)
            assertEquals(HttpMethod.Post, capturedMethod)
            assertTrue(capturedContentType!!.startsWith("application/json"))
        }

    @Test
    fun `generateContent sends the request body as-is`() =
        runBlocking {
            var capturedBody: String? = null
            val api =
                testGeminiVisionApi { req ->
                    capturedBody = (req.body as TextContent).text
                    respondJson(okResponseJson)
                }

            api.generateContent(request)

            assertTrue(capturedBody!!.contains("\"text\":\"describe\""))
        }

    @Test
    fun `generateContent parses the candidate text`() =
        runBlocking {
            val api = testGeminiVisionApi { respondJson(okResponseJson) }

            val response = api.generateContent(request)

            val content = response.candidates.single().content
            val text = content?.parts?.single()?.text
            assertEquals("A warm afternoon wandering the old town together.", text)
        }

    @Test
    fun `HTTP 4xx throws ClientError`() =
        runBlocking<Unit> {
            val api =
                testGeminiVisionApi {
                    respondJson("""{"error": {"message": "invalid api key"}}""", HttpStatusCode.BadRequest)
                }

            assertFailsWith<GeminiException.ClientError> {
                api.generateContent(request)
            }
        }

    @Test
    fun `HTTP 5xx throws ServerError`() =
        runBlocking<Unit> {
            val api =
                testGeminiVisionApi {
                    respondJson("""{"error": {"message": "overloaded"}}""", HttpStatusCode.ServiceUnavailable)
                }

            assertFailsWith<GeminiException.ServerError> {
                api.generateContent(request)
            }
        }

    @Test
    fun `malformed body throws MalformedResponse`() =
        runBlocking<Unit> {
            val api = testGeminiVisionApi { respondJson("not json at all") }

            assertFailsWith<GeminiException.MalformedResponse> {
                api.generateContent(request)
            }
        }
}
