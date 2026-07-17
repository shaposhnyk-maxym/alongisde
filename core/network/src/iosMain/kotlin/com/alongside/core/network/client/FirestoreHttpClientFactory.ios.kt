package com.alongside.core.network.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

public fun createFirestoreHttpClient(): HttpClient = HttpClient(Darwin) { configureFirestoreHttpClient() }
