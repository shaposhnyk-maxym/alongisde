package com.alongside.core.network.auth

public class FirebaseAuthConfig(
    public val apiKey: String,
) {
    public val signInWithIdpUrl: String
        get() = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithIdp?key=$apiKey"
}
