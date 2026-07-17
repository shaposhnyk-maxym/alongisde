package com.alongside.core.model.auth

/**
 * The signed-in person, as reported by the identity provider (Google) via Firebase Auth.
 */
public data class AuthUser(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?,
)
