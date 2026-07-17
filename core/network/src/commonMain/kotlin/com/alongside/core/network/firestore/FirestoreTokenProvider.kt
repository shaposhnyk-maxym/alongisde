package com.alongside.core.network.firestore

/**
 * Seam for injecting a Firebase Auth bearer token once auth exists (milestone M5). Returns `null`
 * when no token is available - the request is then sent without an `Authorization` header.
 */
public fun interface FirestoreTokenProvider {
    public suspend fun currentToken(): String?
}
