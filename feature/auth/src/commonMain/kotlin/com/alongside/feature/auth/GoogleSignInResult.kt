package com.alongside.feature.auth

/** Outcome of the platform's native Google sign-in UI flow. */
public sealed interface GoogleSignInResult {
    public data class Success(
        val idToken: String,
    ) : GoogleSignInResult

    public data object Cancelled : GoogleSignInResult

    public data class Failure(
        val message: String?,
    ) : GoogleSignInResult
}
