package com.alongside.core.domain.auth

import com.alongside.core.model.auth.AuthSession
import com.alongside.core.model.auth.AuthUser
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class AuthSessionExpiryTest {
    private val issuedAt = Instant.fromEpochMilliseconds(1_752_600_000_000)
    private val session =
        AuthSession(
            user = AuthUser(uid = "uid-1", email = null, displayName = null, photoUrl = null),
            idToken = "id-token",
            refreshToken = null,
            expiresInSeconds = 3600L,
            issuedAt = issuedAt,
        )

    @Test
    fun `well within its lifetime is not expired`() {
        assertFalse(session.isExpired(now = issuedAt + 10.minutes))
    }

    @Test
    fun `past its lifetime is expired`() {
        assertTrue(session.isExpired(now = issuedAt + 61.minutes))
    }

    @Test
    fun `inside the safety margin before actual expiry is already treated as expired`() {
        // expiresInSeconds=3600 -> expires at issuedAt+60min; the 5-minute safety margin means
        // a session is treated as expired 5 minutes before that, not exactly at it.
        assertTrue(session.isExpired(now = issuedAt + 56.minutes))
    }

    @Test
    fun `well before the safety margin is not expired`() {
        assertFalse(session.isExpired(now = issuedAt + 54.minutes))
    }
}
