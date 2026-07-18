package com.alongside.core.network.auth.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response of the Secure Token token-exchange endpoint (snake_case, unlike Identity Toolkit).
 * [expiresIn] is a decimal string, same quirk as [FirebaseSignInResponse.expiresIn].
 * See: https://firebase.google.com/docs/reference/rest/auth#section-refresh-token
 */
@Serializable
public data class FirebaseRefreshTokenResponse(
    @SerialName("id_token") public val idToken: String,
    @SerialName("refresh_token") public val refreshToken: String? = null,
    @SerialName("expires_in") public val expiresIn: String? = null,
    @SerialName("user_id") public val userId: String? = null,
)
