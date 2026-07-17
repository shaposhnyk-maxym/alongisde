package com.alongside.core.network.auth.model

import kotlinx.serialization.Serializable

/**
 * Successful `accounts:signInWithIdp` response. `expiresIn` is a decimal string, not a number.
 *
 * `refreshToken`/`expiresIn` are documented as part of the response but are not always present
 * (observed on-device: a response with a valid, correctly-signed `idToken` and no `refreshToken`
 * or `expiresIn` at all) - both are optional here rather than required.
 */
@Serializable
public data class FirebaseSignInResponse(
    public val idToken: String,
    public val refreshToken: String? = null,
    public val expiresIn: String? = null,
    public val localId: String,
    public val email: String? = null,
    public val displayName: String? = null,
    public val photoUrl: String? = null,
)
