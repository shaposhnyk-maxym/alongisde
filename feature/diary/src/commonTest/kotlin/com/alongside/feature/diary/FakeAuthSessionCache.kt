package com.alongside.feature.diary

import com.alongside.core.domain.auth.AuthSessionCache
import com.alongside.core.model.auth.AuthSession
import com.alongside.core.model.auth.AuthUser
import kotlin.time.Instant

internal fun testAuthSession(uid: String = "uid-1"): AuthSession =
    AuthSession(
        user = AuthUser(uid = uid, email = null, displayName = null, photoUrl = null),
        idToken = "id-token",
        refreshToken = null,
        expiresInSeconds = 3600L,
        issuedAt = Instant.fromEpochMilliseconds(0),
    )

internal class FakeAuthSessionCache(
    private var session: AuthSession? = testAuthSession(),
) : AuthSessionCache {
    override suspend fun get(): AuthSession? = session

    override suspend fun save(session: AuthSession) {
        this.session = session
    }

    override suspend fun clear() {
        session = null
    }
}
