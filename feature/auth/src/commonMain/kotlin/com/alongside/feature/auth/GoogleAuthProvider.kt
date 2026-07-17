package com.alongside.feature.auth

/**
 * Seam around the platform's native Google sign-in SDK (Credential Manager on Android, GIDSignIn
 * on iOS). Deliberately callback-based rather than `suspend` so a Swift class can implement it
 * directly once an iOS app target exists - `AuthContainer` adapts the callback to a coroutine.
 */
public interface GoogleAuthProvider {
    public fun signIn(onResult: (GoogleSignInResult) -> Unit)

    /**
     * Like [signIn] but never shows UI: resolves only if there's already an account that
     * previously authorized this app, otherwise fails as [GoogleSignInResult.Failure]. Used to
     * silently refresh an expired cached session without interrupting the user.
     */
    public fun signInSilently(onResult: (GoogleSignInResult) -> Unit)
}
