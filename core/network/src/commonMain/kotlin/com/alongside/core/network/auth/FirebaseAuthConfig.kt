package com.alongside.core.network.auth

public class FirebaseAuthConfig(
    public val apiKey: String,
) {
    public val signInWithIdpUrl: String
        get() = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithIdp?key=$apiKey"

    public val secureTokenUrl: String
        get() = "https://securetoken.googleapis.com/v1/token?key=$apiKey"
}
