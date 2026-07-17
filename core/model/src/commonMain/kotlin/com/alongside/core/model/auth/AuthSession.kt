package com.alongside.core.model.auth

import kotlin.time.Instant

/**
 * A Firebase Auth session obtained by exchanging a Google ID token via the Identity Toolkit
 * REST API's `accounts:signInWithIdp` endpoint. [refreshToken] is nullable because that endpoint
 * doesn't always include one (observed on-device), unlike [idToken]. [issuedAt] is when this
 * exchange happened, used with [expiresInSeconds] to know when [idToken] needs replacing.
 */
data class AuthSession(
    val user: AuthUser,
    val idToken: String,
    val refreshToken: String?,
    val expiresInSeconds: Long,
    val issuedAt: Instant,
)
