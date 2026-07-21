package com.alongside.core.network.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

public fun createFirestoreHttpClient(): HttpClient = HttpClient(OkHttp) { configureFirestoreHttpClient() }

/** See [com.alongside.core.network.places.KtorShareLinkRedirectResolver]'s kdoc for why `followRedirects = false`. */
public fun createShareLinkRedirectHttpClient(): HttpClient =
    HttpClient(OkHttp) {
        followRedirects = false
        configureFirestoreHttpClient()
    }
