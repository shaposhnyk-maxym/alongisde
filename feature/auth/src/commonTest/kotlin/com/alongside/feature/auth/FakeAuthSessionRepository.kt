package com.alongside.feature.auth

import com.alongside.core.domain.auth.AuthException
import com.alongside.core.domain.auth.AuthSessionRepository
import com.alongside.core.model.auth.AuthSession

internal class FakeAuthSessionRepository(
    private val result: Result<AuthSession>,
) : AuthSessionRepository {
    override suspend fun signInWithGoogle(googleIdToken: String): AuthSession = result.getOrThrow()

    internal companion object {
        internal fun success(session: AuthSession) = FakeAuthSessionRepository(Result.success(session))

        internal fun failure(exception: AuthException) = FakeAuthSessionRepository(Result.failure(exception))
    }
}
