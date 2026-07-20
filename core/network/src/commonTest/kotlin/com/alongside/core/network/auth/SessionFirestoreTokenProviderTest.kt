package com.alongside.core.network.auth

import com.alongside.core.domain.auth.AuthSessionCache
import com.alongside.core.model.auth.AuthSession
import com.alongside.core.model.auth.AuthUser
import com.alongside.core.network.auth.model.FirebaseRefreshTokenResponse
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

private val FIXED_NOW = Instant.fromEpochMilliseconds(1_752_800_000_000)

private object FixedClock : Clock {
    override fun now(): Instant = FIXED_NOW
}

private class FakeAuthSessionCache : AuthSessionCache {
    var current: AuthSession? = null
    var saved = mutableListOf<AuthSession>()
    var getFailure: Exception? = null

    override suspend fun get(): AuthSession? {
        getFailure?.let { throw it }
        return current
    }

    override suspend fun save(session: AuthSession) {
        saved += session
        current = session
    }

    override suspend fun clear() {
        current = null
    }
}

private class ScriptedIdTokenRefresher : IdTokenRefresher {
    var response: FirebaseRefreshTokenResponse? = null
    var failure: FirebaseAuthException? = null
    val refreshedWith = mutableListOf<String>()

    override suspend fun refresh(refreshToken: String): FirebaseRefreshTokenResponse {
        refreshedWith += refreshToken
        failure?.let { throw it }
        return checkNotNull(response)
    }
}

class SessionFirestoreTokenProviderTest {
    private val cache = FakeAuthSessionCache()
    private val refresher = ScriptedIdTokenRefresher()
    private val provider = SessionFirestoreTokenProvider(cache, refresher, FixedClock)

    private fun session(
        idToken: String = "cached-id-token",
        refreshToken: String? = "stored-refresh-token",
        issuedAt: Instant = FIXED_NOW,
        expiresInSeconds: Long = 3600,
    ) = AuthSession(
        user = AuthUser(uid = "uid-1", email = null, displayName = null, photoUrl = null),
        idToken = idToken,
        refreshToken = refreshToken,
        expiresInSeconds = expiresInSeconds,
        issuedAt = issuedAt,
    )

    @Test
    fun `no cached session yields null without refreshing`() =
        runTest {
            assertNull(provider.currentToken())
            assertEquals(emptyList(), refresher.refreshedWith)
        }

    @Test
    fun `a valid cached session yields its idToken without refreshing`() =
        runTest {
            cache.current = session()

            assertEquals("cached-id-token", provider.currentToken())
            assertEquals(emptyList(), refresher.refreshedWith)
        }

    @Test
    fun `an expired session is refreshed and the rotated session saved`() =
        runTest {
            cache.current = session(issuedAt = FIXED_NOW - 2.hours)
            refresher.response =
                FirebaseRefreshTokenResponse(
                    idToken = "new-id-token",
                    refreshToken = "rotated-refresh-token",
                    expiresIn = "7200",
                )

            assertEquals("new-id-token", provider.currentToken())

            assertEquals(listOf("stored-refresh-token"), refresher.refreshedWith)
            val saved = cache.saved.single()
            assertEquals("new-id-token", saved.idToken)
            assertEquals("rotated-refresh-token", saved.refreshToken)
            assertEquals(7200, saved.expiresInSeconds)
            assertEquals(FIXED_NOW, saved.issuedAt)
            assertEquals("uid-1", saved.user.uid)
        }

    @Test
    fun `a refresh response without a rotated token keeps the stored refresh token`() =
        runTest {
            cache.current = session(issuedAt = FIXED_NOW - 2.hours)
            refresher.response = FirebaseRefreshTokenResponse(idToken = "new-id-token")

            provider.currentToken()

            val saved = cache.saved.single()
            assertEquals("stored-refresh-token", saved.refreshToken)
            assertEquals(3600, saved.expiresInSeconds)
        }

    @Test
    fun `an expired session without a refresh token yields null without refreshing`() =
        runTest {
            cache.current = session(refreshToken = null, issuedAt = FIXED_NOW - 2.hours)

            assertNull(provider.currentToken())
            assertEquals(emptyList(), refresher.refreshedWith)
        }

    @Test
    fun `a local cache read failure yields null instead of throwing uncaught`() =
        runTest {
            cache.getFailure = IllegalStateException("simulated local database I/O failure")

            assertNull(provider.currentToken())
            assertEquals(emptyList(), refresher.refreshedWith)
        }

    @Test
    fun `a failed refresh yields null and leaves the cached session untouched`() =
        runTest {
            val expired = session(issuedAt = FIXED_NOW - 2.hours)
            cache.current = expired
            refresher.failure = FirebaseAuthException.ServerError(code = 503, status = "UNAVAILABLE", message = "down")

            assertNull(provider.currentToken())
            assertEquals(expired, cache.current)
            assertEquals(emptyList(), cache.saved)
        }
}
