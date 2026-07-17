package com.alongside.feature.auth.presentation

import com.alongside.core.domain.auth.AuthException
import com.alongside.core.model.auth.AuthSession
import com.alongside.core.model.auth.AuthUser
import com.alongside.feature.auth.FakeAuthSessionCache
import com.alongside.feature.auth.FakeAuthSessionRepository
import com.alongside.feature.auth.FakeGoogleAuthProvider
import com.alongside.feature.auth.GoogleSignInResult
import kotlinx.coroutines.test.runTest
import org.orbitmvi.orbit.test.test
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

class AuthContainerTest {
    private val testSession =
        AuthSession(
            user = AuthUser(uid = "uid-1", email = "person@example.com", displayName = "Person One", photoUrl = null),
            idToken = "firebase-id-token",
            refreshToken = "firebase-refresh-token",
            expiresInSeconds = 3600L,
            issuedAt = Clock.System.now(),
        )

    @Test
    fun `initial state is idle when nothing is cached`() =
        runTest {
            val container =
                AuthContainer(
                    FakeGoogleAuthProvider(GoogleSignInResult.Cancelled),
                    FakeAuthSessionRepository.success(testSession),
                    FakeAuthSessionCache(),
                )

            container.test(this) {
                // autoCheckInitialState (default, orbit-test 10) already asserted this on entry.
            }
        }

    @Test
    fun `successful google sign-in persists the session and transitions to signed-in state`() =
        runTest {
            val cache = FakeAuthSessionCache()
            val container =
                AuthContainer(
                    FakeGoogleAuthProvider(GoogleSignInResult.Success("google-id-token")),
                    FakeAuthSessionRepository.success(testSession),
                    cache,
                )

            container.test(this) {
                containerHost.onIntent(AuthIntent.SignInWithGoogle)
                expectState { copy(isSigningIn = true) }
                expectState { copy(isSigningIn = false, session = testSession) }
                expectSideEffect(AuthSideEffect.SignedIn)
            }
            assertEquals(testSession, cache.current)
        }

    @Test
    fun `user cancelling the native sign-in flow returns to idle without an error`() =
        runTest {
            val container =
                AuthContainer(
                    FakeGoogleAuthProvider(GoogleSignInResult.Cancelled),
                    FakeAuthSessionRepository.success(testSession),
                    FakeAuthSessionCache(),
                )

            container.test(this) {
                containerHost.onIntent(AuthIntent.SignInWithGoogle)
                expectState { copy(isSigningIn = true) }
                expectState { copy(isSigningIn = false) }
            }
        }

    @Test
    fun `network failure while exchanging the token surfaces a NETWORK error`() =
        runTest {
            val container =
                AuthContainer(
                    FakeGoogleAuthProvider(GoogleSignInResult.Success("google-id-token")),
                    FakeAuthSessionRepository.failure(AuthException.Network()),
                    FakeAuthSessionCache(),
                )

            container.test(this) {
                containerHost.onIntent(AuthIntent.SignInWithGoogle)
                expectState { copy(isSigningIn = true) }
                expectState { copy(isSigningIn = false, error = AuthError.NETWORK) }
            }
        }

    @Test
    fun `invalid token from firebase surfaces an INVALID_TOKEN error`() =
        runTest {
            val container =
                AuthContainer(
                    FakeGoogleAuthProvider(GoogleSignInResult.Success("google-id-token")),
                    FakeAuthSessionRepository.failure(AuthException.InvalidToken()),
                    FakeAuthSessionCache(),
                )

            container.test(this) {
                containerHost.onIntent(AuthIntent.SignInWithGoogle)
                expectState { copy(isSigningIn = true) }
                expectState { copy(isSigningIn = false, error = AuthError.INVALID_TOKEN) }
            }
        }

    @Test
    fun `provider failure surfaces a SIGN_IN_FAILED error`() =
        runTest {
            val container =
                AuthContainer(
                    FakeGoogleAuthProvider(GoogleSignInResult.Failure("boom")),
                    FakeAuthSessionRepository.success(testSession),
                    FakeAuthSessionCache(),
                )

            container.test(this) {
                containerHost.onIntent(AuthIntent.SignInWithGoogle)
                expectState { copy(isSigningIn = true) }
                expectState { copy(isSigningIn = false, error = AuthError.SIGN_IN_FAILED) }
            }
        }

    @Test
    fun `dismissing an error clears it`() =
        runTest {
            val container =
                AuthContainer(
                    FakeGoogleAuthProvider(GoogleSignInResult.Failure("boom")),
                    FakeAuthSessionRepository.success(testSession),
                    FakeAuthSessionCache(),
                )

            container.test(this) {
                containerHost.onIntent(AuthIntent.SignInWithGoogle)
                expectState { copy(isSigningIn = true) }
                expectState { copy(isSigningIn = false, error = AuthError.SIGN_IN_FAILED) }
                containerHost.onIntent(AuthIntent.DismissError)
                expectState { copy(error = null) }
            }
        }

    @Test
    fun `a non-expired cached session is restored into state on start without any network call`() =
        runTest {
            val cache = FakeAuthSessionCache(initial = testSession)
            val container =
                AuthContainer(
                    FakeGoogleAuthProvider(GoogleSignInResult.Failure("should not be called")),
                    FakeAuthSessionRepository.failure(AuthException.Unknown()),
                    cache,
                )

            container.test(this) {
                runOnCreate()
                expectState { copy(session = testSession) }
                expectSideEffect(AuthSideEffect.SignedIn)
            }
        }

    @Test
    fun `an expired cached session is silently refreshed via signInSilently`() =
        runTest {
            val expiredSession = testSession.copy(issuedAt = Clock.System.now() - 120.minutes)
            val refreshedSession = testSession.copy(idToken = "fresh-firebase-id-token")
            val cache = FakeAuthSessionCache(initial = expiredSession)
            val container =
                AuthContainer(
                    FakeGoogleAuthProvider(
                        result = GoogleSignInResult.Failure("explicit sign-in should not run"),
                        silentResult = GoogleSignInResult.Success("fresh-google-id-token"),
                    ),
                    FakeAuthSessionRepository.success(refreshedSession),
                    cache,
                )

            container.test(this) {
                runOnCreate()
                expectState { copy(session = refreshedSession) }
                expectSideEffect(AuthSideEffect.SignedIn)
            }
            assertEquals(refreshedSession, cache.current)
        }

    @Test
    fun `silent refresh failing at the provider clears the cache and stays idle without an error banner`() =
        runTest {
            val expiredSession = testSession.copy(issuedAt = Clock.System.now() - 120.minutes)
            val cache = FakeAuthSessionCache(initial = expiredSession)
            val container =
                AuthContainer(
                    FakeGoogleAuthProvider(
                        result = GoogleSignInResult.Failure("explicit sign-in should not run"),
                        silentResult = GoogleSignInResult.Cancelled,
                    ),
                    FakeAuthSessionRepository.success(testSession),
                    cache,
                )

            container.test(this) {
                runOnCreate()
                // No state change - the cache is cleared, but a background restore attempt
                // shouldn't surface an error banner the user never asked to see.
            }
            assertNull(cache.current)
            assertTrue(cache.wasCleared)
        }

    @Test
    fun `silent refresh failing during token exchange clears the cache and stays idle without an error banner`() =
        runTest {
            val expiredSession = testSession.copy(issuedAt = Clock.System.now() - 120.minutes)
            val cache = FakeAuthSessionCache(initial = expiredSession)
            val container =
                AuthContainer(
                    FakeGoogleAuthProvider(
                        result = GoogleSignInResult.Failure("explicit sign-in should not run"),
                        silentResult = GoogleSignInResult.Success("fresh-google-id-token"),
                    ),
                    FakeAuthSessionRepository.failure(AuthException.Network()),
                    cache,
                )

            container.test(this) {
                runOnCreate()
            }
            assertNull(cache.current)
            assertTrue(cache.wasCleared)
        }
}
