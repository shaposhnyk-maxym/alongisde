package com.alongside.core.network.auth

import io.ktor.content.TextContent
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

// runBlocking, not runTest: runTest's virtual-time scheduler falsely times out
// Ktor HttpTimeout against MockEngine (see the M3 note in docs/roadmap.md).
class FirebaseAuthRefreshTest {
    private val refreshResponseJson =
        """
        {
          "access_token": "new-id-token",
          "expires_in": "3600",
          "token_type": "Bearer",
          "refresh_token": "rotated-refresh-token",
          "id_token": "new-id-token",
          "user_id": "uid-1",
          "project_id": "000000000000"
        }
        """.trimIndent()

    @Test
    fun `refreshIdToken posts a form-encoded grant to the securetoken endpoint`() =
        runBlocking {
            var capturedUrl: String? = null
            var capturedMethod: HttpMethod? = null
            var capturedBody: String? = null
            var capturedContentType: String? = null
            val api =
                testFirebaseAuthApi { request ->
                    capturedUrl = request.url.toString()
                    capturedMethod = request.method
                    capturedBody = (request.body as TextContent).text
                    capturedContentType = request.body.contentType.toString()
                    respondJson(refreshResponseJson)
                }

            api.refreshIdToken("stored-refresh-token")

            assertEquals("https://securetoken.googleapis.com/v1/token?key=test-api-key", capturedUrl)
            assertEquals(HttpMethod.Post, capturedMethod)
            val contentType = assertNotNull(capturedContentType)
            assertTrue(contentType.startsWith("application/x-www-form-urlencoded"))
            assertEquals("grant_type=refresh_token&refresh_token=stored-refresh-token", capturedBody)
        }

    @Test
    fun `refreshIdToken parses the snake_case response`() =
        runBlocking {
            val api = testFirebaseAuthApi { respondJson(refreshResponseJson) }

            val response = api.refreshIdToken("stored-refresh-token")

            assertEquals("new-id-token", response.idToken)
            assertEquals("rotated-refresh-token", response.refreshToken)
            assertEquals("3600", response.expiresIn)
            assertEquals("uid-1", response.userId)
        }

    @Test
    fun `refreshIdToken with an invalid refresh token throws ClientError`() =
        runBlocking<Unit> {
            val api =
                testFirebaseAuthApi {
                    respondJson(
                        """{"error":{"code":400,"message":"INVALID_REFRESH_TOKEN","status":"INVALID_ARGUMENT"}}""",
                        HttpStatusCode.BadRequest,
                    )
                }

            val exception =
                assertFailsWith<FirebaseAuthException.ClientError> {
                    api.refreshIdToken("stale-refresh-token")
                }
            assertEquals("INVALID_REFRESH_TOKEN", exception.message)
        }

    @Test
    fun `refreshIdToken maps a 5xx response to ServerError`() =
        runBlocking<Unit> {
            val api =
                testFirebaseAuthApi {
                    respondJson(
                        """{"error":{"code":503,"message":"BACKEND_ERROR","status":"UNAVAILABLE"}}""",
                        HttpStatusCode.ServiceUnavailable,
                    )
                }

            assertFailsWith<FirebaseAuthException.ServerError> {
                api.refreshIdToken("stored-refresh-token")
            }
        }

    @Test
    fun `refreshIdToken with a malformed body throws MalformedResponse`() =
        runBlocking<Unit> {
            val api = testFirebaseAuthApi { respondJson("not json at all") }

            assertFailsWith<FirebaseAuthException.MalformedResponse> {
                api.refreshIdToken("stored-refresh-token")
            }
        }
}
