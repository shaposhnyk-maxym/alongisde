package com.alongside.core.network.auth.model

import kotlinx.serialization.Serializable

/** Body for `accounts:signInWithIdp`, exchanging a Google ID token for a Firebase session. */
@Serializable
public data class FirebaseSignInRequest(
    public val postBody: String,
    public val requestUri: String,
    public val returnSecureToken: Boolean = true,
    public val returnIdpCredential: Boolean = true,
) {
    public companion object {
        public fun forGoogleIdToken(googleIdToken: String): FirebaseSignInRequest =
            FirebaseSignInRequest(
                postBody = "id_token=$googleIdToken&providerId=google.com",
                requestUri = "http://localhost",
            )
    }
}
