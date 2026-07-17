package com.alongside.core.network.client

import com.alongside.core.network.firestore.model.firestoreJson
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

public const val DEFAULT_FIRESTORE_REQUEST_TIMEOUT_MILLIS: Long = 15_000L

/**
 * Engine-agnostic pipeline shared by every platform's real [io.ktor.client.HttpClient] factory and
 * by tests building a client over Ktor's `MockEngine` - both go through this function so tests
 * exercise the same configuration production uses.
 */
public fun HttpClientConfig<*>.configureFirestoreHttpClient(
    json: Json = firestoreJson,
    requestTimeoutMillis: Long = DEFAULT_FIRESTORE_REQUEST_TIMEOUT_MILLIS,
    enableLogging: Boolean = false,
) {
    expectSuccess = false
    install(ContentNegotiation) { json(json) }
    install(HttpTimeout) {
        this.requestTimeoutMillis = requestTimeoutMillis
        connectTimeoutMillis = requestTimeoutMillis
        socketTimeoutMillis = requestTimeoutMillis
    }
    if (enableLogging) {
        install(Logging) { level = LogLevel.INFO }
    }
}
