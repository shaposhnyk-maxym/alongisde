package com.alongside.core.network.auth

import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FirebaseAuthApiTest {
    private val signInResponseJson =
        """
        {
          "idToken": "firebase-id-token",
          "refreshToken": "firebase-refresh-token",
          "expiresIn": "3600",
          "localId": "uid-1",
          "email": "person@example.com",
          "displayName": "Person One",
          "photoUrl": "https://example.com/photo.jpg"
        }
        """.trimIndent()

    @Test
    fun `signInWithGoogleIdToken success parses the response and sends the api key`() =
        runBlocking {
            var capturedUrl: String? = null
            val api =
                testFirebaseAuthApi { request ->
                    capturedUrl = request.url.toString()
                    respondJson(signInResponseJson)
                }

            val response = api.signInWithGoogleIdToken("google-id-token")

            assertEquals("firebase-id-token", response.idToken)
            assertEquals("firebase-refresh-token", response.refreshToken)
            assertEquals("3600", response.expiresIn)
            assertEquals("uid-1", response.localId)
            val url = assertNotNull(capturedUrl)
            assertTrue(url.contains("key=test-api-key"))
        }

    @Test
    fun `signInWithGoogleIdToken parses a response missing refreshToken and expiresIn`() =
        runBlocking {
            val api =
                testFirebaseAuthApi {
                    respondJson(
                        """
                        {
                          "idToken": "firebase-id-token",
                          "localId": "uid-1",
                          "email": "person@example.com",
                          "kind": "identitytoolkit#VerifyAssertionResponse"
                        }
                        """.trimIndent(),
                    )
                }

            val response = api.signInWithGoogleIdToken("google-id-token")

            assertEquals("firebase-id-token", response.idToken)
            assertEquals(null, response.refreshToken)
            assertEquals(null, response.expiresIn)
        }

    @Test
    fun `signInWithGoogleIdToken with invalid idp response throws InvalidIdToken`() =
        runBlocking<Unit> {
            val api =
                testFirebaseAuthApi {
                    respondJson(
                        """{"error":{"code":400,"message":"INVALID_IDP_RESPONSE","status":"INVALID_ARGUMENT"}}""",
                        HttpStatusCode.BadRequest,
                    )
                }

            assertFailsWith<FirebaseAuthException.InvalidIdToken> {
                api.signInWithGoogleIdToken("bad-token")
            }
        }

    @Test
    fun `signInWithGoogleIdToken with invalid id token message throws InvalidIdToken`() =
        runBlocking<Unit> {
            val api =
                testFirebaseAuthApi {
                    respondJson(
                        """{"error":{"code":400,"message":"INVALID_ID_TOKEN","status":"INVALID_ARGUMENT"}}""",
                        HttpStatusCode.BadRequest,
                    )
                }

            assertFailsWith<FirebaseAuthException.InvalidIdToken> {
                api.signInWithGoogleIdToken("bad-token")
            }
        }

    @Test
    fun `signInWithGoogleIdToken with other 400 throws ClientError, not InvalidIdToken`() =
        runBlocking {
            val api =
                testFirebaseAuthApi {
                    respondJson(
                        """{"error":{"code":400,"message":"MISSING_REQUEST_URI","status":"INVALID_ARGUMENT"}}""",
                        HttpStatusCode.BadRequest,
                    )
                }

            val error =
                assertFailsWith<FirebaseAuthException.ClientError> {
                    api.signInWithGoogleIdToken("bad-token")
                }
            assertEquals(400, error.code)
        }

    @Test
    fun `signInWithGoogleIdToken with 500 throws ServerError`() =
        runBlocking {
            val api =
                testFirebaseAuthApi {
                    respondJson(
                        """{"error":{"code":500,"message":"internal error","status":"INTERNAL"}}""",
                        HttpStatusCode.InternalServerError,
                    )
                }

            val error =
                assertFailsWith<FirebaseAuthException.ServerError> {
                    api.signInWithGoogleIdToken("google-id-token")
                }
            assertEquals(500, error.code)
        }

    @Test
    fun `signInWithGoogleIdToken with malformed JSON body throws MalformedResponse`() =
        runBlocking<Unit> {
            val api = testFirebaseAuthApi { respondJson("""{"idToken": not valid json""") }

            assertFailsWith<FirebaseAuthException.MalformedResponse> {
                api.signInWithGoogleIdToken("google-id-token")
            }
        }

    @Test
    fun `signInWithGoogleIdToken that never responds in time throws NetworkTimeout`() =
        runBlocking<Unit> {
            val api =
                testFirebaseAuthApi(requestTimeoutMillis = 200L) {
                    delay(2_000)
                    respondJson(signInResponseJson)
                }

            assertFailsWith<FirebaseAuthException.NetworkTimeout> {
                api.signInWithGoogleIdToken("google-id-token")
            }
        }
}
