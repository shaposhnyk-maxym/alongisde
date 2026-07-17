package com.alongside.feature.auth

internal class FakeGoogleAuthProvider(
    private val result: GoogleSignInResult,
    private val silentResult: GoogleSignInResult = result,
) : GoogleAuthProvider {
    override fun signIn(onResult: (GoogleSignInResult) -> Unit) {
        onResult(result)
    }

    override fun signInSilently(onResult: (GoogleSignInResult) -> Unit) {
        onResult(silentResult)
    }
}
