package com.alongside.core.network.firestore.model

import kotlinx.serialization.json.Json

/** Shared JSON config so production parsing and tests decode/encode identically. */
public val firestoreJson: Json =
    Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }
