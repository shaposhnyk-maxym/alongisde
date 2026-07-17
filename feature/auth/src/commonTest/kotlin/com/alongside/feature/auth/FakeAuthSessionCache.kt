package com.alongside.feature.auth

import com.alongside.core.domain.auth.AuthSessionCache
import com.alongside.core.model.auth.AuthSession

internal class FakeAuthSessionCache(
    initial: AuthSession? = null,
) : AuthSessionCache {
    var current: AuthSession? = initial
        private set
    var wasCleared: Boolean = false
        private set

    override suspend fun get(): AuthSession? = current

    override suspend fun save(session: AuthSession) {
        current = session
        wasCleared = false
    }

    override suspend fun clear() {
        current = null
        wasCleared = true
    }
}
