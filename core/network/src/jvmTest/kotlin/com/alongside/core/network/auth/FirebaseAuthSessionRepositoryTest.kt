package com.alongside.core.network.auth

import com.alongside.core.domain.auth.AuthException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FirebaseAuthSessionRepositoryTest {
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
    fun `signInWithGoogle maps a successful response to an AuthSession`() =
        runBlocking {
            val repository = FirebaseAuthSessionRepository(testFirebaseAuthApi { respondJson(signInResponseJson) })

            val session = repository.signInWithGoogle("google-id-token")

            assertEquals("uid-1", session.user.uid)
            assertEquals("person@example.com", session.user.email)
            assertEquals("Person One", session.user.displayName)
            assertEquals("https://example.com/photo.jpg", session.user.photoUrl)
            assertEquals("firebase-id-token", session.idToken)
            assertEquals("firebase-refresh-token", session.refreshToken)
            assertEquals(3600L, session.expiresInSeconds)
        }

    @Test
    fun `signInWithGoogle defaults refreshToken to null and expiresInSeconds to 3600 when absent`() =
        runBlocking {
            val repository =
                FirebaseAuthSessionRepository(
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
                    },
                )

            val session = repository.signInWithGoogle("google-id-token")

            assertEquals("firebase-id-token", session.idToken)
            assertEquals(null, session.refreshToken)
            assertEquals(3600L, session.expiresInSeconds)
        }

    @Test
    fun `signInWithGoogle maps an invalid idp response to AuthException InvalidToken`() =
        runBlocking<Unit> {
            val repository =
                FirebaseAuthSessionRepository(
                    testFirebaseAuthApi {
                        respondJson(
                            """{"error":{"code":400,"message":"INVALID_IDP_RESPONSE","status":"INVALID_ARGUMENT"}}""",
                            HttpStatusCode.BadRequest,
                        )
                    },
                )

            assertFailsWith<AuthException.InvalidToken> { repository.signInWithGoogle("bad-token") }
        }

    @Test
    fun `signInWithGoogle maps a timeout to AuthException Network`() =
        runBlocking<Unit> {
            val repository =
                FirebaseAuthSessionRepository(
                    testFirebaseAuthApi(requestTimeoutMillis = 200L) {
                        delay(2_000)
                        respondJson(signInResponseJson)
                    },
                )

            assertFailsWith<AuthException.Network> { repository.signInWithGoogle("google-id-token") }
        }

    @Test
    fun `signInWithGoogle maps a server error to AuthException Unknown`() =
        runBlocking<Unit> {
            val repository =
                FirebaseAuthSessionRepository(
                    testFirebaseAuthApi {
                        respondJson(
                            """{"error":{"code":500,"message":"internal error","status":"INTERNAL"}}""",
                            HttpStatusCode.InternalServerError,
                        )
                    },
                )

            assertFailsWith<AuthException.Unknown> { repository.signInWithGoogle("google-id-token") }
        }
}
