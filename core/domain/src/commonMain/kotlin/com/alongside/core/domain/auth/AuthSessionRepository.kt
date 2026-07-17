package com.alongside.core.domain.auth

import com.alongside.core.model.auth.AuthSession

/**
 * Exchanges a Google ID token (already obtained from the platform sign-in SDK) for a Firebase
 * Auth session. Throws [AuthException] on failure.
 */
public interface AuthSessionRepository {
    public suspend fun signInWithGoogle(googleIdToken: String): AuthSession
}
