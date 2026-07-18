package com.alongside.core.network.firestore.model

import kotlinx.serialization.json.Json

/** Shared JSON config so production parsing and tests decode/encode identically. */
public val firestoreJson: Json =
    Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        // Without this, kotlinx.serialization omits fields still at their declared default -
        // e.g. FirebaseSignInRequest.returnSecureToken/returnIdpCredential (both default true)
        // silently vanished from the request body, so Identity Toolkit never saw
        // returnSecureToken=true and minted a bare identitytoolkit.google.com-issued token
        // instead of a proper securetoken.google.com Firebase session (no refreshToken/expiresIn
        // either) - which Firestore then rejects outright with 401 UNAUTHENTICATED.
        encodeDefaults = true
    }
