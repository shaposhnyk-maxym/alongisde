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
import kotlin.time.Duration.Companion.seconds

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
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent" +
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
    fun `parsing tolerates real-world response fields not modeled in our DTOs`() =
        runBlocking {
            // Trimmed excerpt of a real gemini-flash-latest (gemini-3.5-flash) response captured
            // during M10 manual testing - thoughtSignature/usageMetadata/role/modelVersion/
            // responseId aren't in our DTOs; ignoreUnknownKeys must tolerate all of them.
            val realResponseJson =
                """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [{"text": "A warm afternoon together.", "thoughtSignature": "abc123=="}],
                        "role": "model"
                      },
                      "finishReason": "STOP",
                      "index": 0
                    }
                  ],
                  "usageMetadata": {"promptTokenCount": 4319, "candidatesTokenCount": 104, "totalTokenCount": 5268},
                  "modelVersion": "gemini-3.5-flash",
                  "responseId": "_flbaqajE9_qnsEPj-_-uA8"
                }
                """.trimIndent()
            val api = testGeminiVisionApi { respondJson(realResponseJson) }

            val response = api.generateContent(request)

            val content = response.candidates.single().content
            assertEquals("A warm afternoon together.", content?.parts?.single()?.text)
        }

    @Test
    fun `HTTP 4xx throws ClientError`() =
        runBlocking<Unit> {
            val api =
                testGeminiVisionApi {
                    respondJson(
                        """{"error": {"code": 400, "message": "invalid api key", "status": "INVALID_ARGUMENT"}}""",
                        HttpStatusCode.BadRequest,
                    )
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
                    respondJson(
                        """{"error": {"code": 503, "message": "overloaded", "status": "UNAVAILABLE"}}""",
                        HttpStatusCode.ServiceUnavailable,
                    )
                }

            assertFailsWith<GeminiException.ServerError> {
                api.generateContent(request)
            }
        }

    @Test
    fun `HTTP error surfaces the real error body message, not the generic HTTP reason`() =
        runBlocking<Unit> {
            // The exact shape returned by a real 429 seen during M10 manual testing (billing exhausted).
            val errorJson =
                """{"error": {"code": 429, "message": "Your prepayment credits are depleted.", """ +
                    """"status": "RESOURCE_EXHAUSTED"}}"""
            val api = testGeminiVisionApi { respondJson(errorJson, HttpStatusCode.TooManyRequests) }

            val exception =
                assertFailsWith<GeminiException.ClientError> {
                    api.generateContent(request)
                }
            assertEquals("Your prepayment credits are depleted.", exception.message)
        }

    @Test
    fun `HTTP error with a body that doesn't match the error envelope falls back to the HTTP reason`() =
        runBlocking<Unit> {
            val api = testGeminiVisionApi { respondJson("""{"unexpected": "shape"}""", HttpStatusCode.BadRequest) }

            val exception =
                assertFailsWith<GeminiException.ClientError> {
                    api.generateContent(request)
                }
            assertEquals(HttpStatusCode.BadRequest.description, exception.message)
        }

    @Test
    fun `malformed body throws MalformedResponse`() =
        runBlocking<Unit> {
            val api = testGeminiVisionApi { respondJson("not json at all") }

            assertFailsWith<GeminiException.MalformedResponse> {
                api.generateContent(request)
            }
        }

    // --- Retry on transient (5xx) failures ---

    @Test
    fun `retries a 503 and succeeds once the server recovers`() =
        runBlocking {
            var callCount = 0
            val api =
                testGeminiVisionApi {
                    callCount++
                    if (callCount == 1) {
                        respondJson(
                            """{"error": {"code": 503, "message": "overloaded", "status": "UNAVAILABLE"}}""",
                            HttpStatusCode.ServiceUnavailable,
                        )
                    } else {
                        respondJson(okResponseJson)
                    }
                }

            val response = api.generateContent(request)

            assertEquals(2, callCount)
            assertEquals(
                "A warm afternoon wandering the old town together.",
                response.candidates
                    .single()
                    .content
                    ?.parts
                    ?.single()
                    ?.text,
            )
        }

    @Test
    fun `gives up after maxAttempts of persistent 503s and throws ServerError`() =
        runBlocking<Unit> {
            var callCount = 0
            val api =
                testGeminiVisionApi(maxAttempts = 3) {
                    callCount++
                    respondJson(
                        """{"error": {"code": 503, "message": "overloaded", "status": "UNAVAILABLE"}}""",
                        HttpStatusCode.ServiceUnavailable,
                    )
                }

            assertFailsWith<GeminiException.ServerError> {
                api.generateContent(request)
            }
            assertEquals(3, callCount)
        }

    @Test
    fun `sleeps between retries with an increasing backoff`() =
        runBlocking {
            var callCount = 0
            val sleptDurations = mutableListOf<kotlin.time.Duration>()
            val api =
                testGeminiVisionApi(maxAttempts = 3, sleep = { sleptDurations += it }) {
                    callCount++
                    if (callCount < 3) {
                        respondJson(
                            """{"error": {"code": 503, "message": "overloaded", "status": "UNAVAILABLE"}}""",
                            HttpStatusCode.ServiceUnavailable,
                        )
                    } else {
                        respondJson(okResponseJson)
                    }
                }

            api.generateContent(request)

            assertEquals(2, sleptDurations.size)
            assertTrue(sleptDurations[0] >= 1.seconds)
            assertTrue(sleptDurations[1] >= 2.seconds)
        }

    @Test
    fun `does not retry a 4xx client error`() =
        runBlocking<Unit> {
            var callCount = 0
            val api =
                testGeminiVisionApi {
                    callCount++
                    respondJson(
                        """{"error": {"code": 400, "message": "invalid api key", "status": "INVALID_ARGUMENT"}}""",
                        HttpStatusCode.BadRequest,
                    )
                }

            assertFailsWith<GeminiException.ClientError> {
                api.generateContent(request)
            }
            assertEquals(1, callCount)
        }

    @Test
    fun `does not retry a malformed response`() =
        runBlocking<Unit> {
            var callCount = 0
            val api =
                testGeminiVisionApi {
                    callCount++
                    respondJson("not json at all")
                }

            assertFailsWith<GeminiException.MalformedResponse> {
                api.generateContent(request)
            }
            assertEquals(1, callCount)
        }
}
