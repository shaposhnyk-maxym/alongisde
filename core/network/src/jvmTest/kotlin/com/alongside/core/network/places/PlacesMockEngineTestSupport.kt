package com.alongside.core.network.places

import com.alongside.core.network.client.configureFirestoreHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf

internal fun MockRequestHandleScope.respondJson(
    json: String,
    status: HttpStatusCode = HttpStatusCode.OK,
): HttpResponseData =
    respond(
        content = json,
        status = status,
        headers = headersOf("Content-Type", ContentType.Application.Json.toString()),
    )

internal fun testGooglePlacesGeocodingApi(
    requestTimeoutMillis: Long = 15_000L,
    handler: suspend MockRequestHandleScope.(request: HttpRequestData) -> HttpResponseData,
): GooglePlacesGeocodingApi {
    val engine = MockEngine { request -> handler(request) }
    val httpClient = HttpClient(engine) { configureFirestoreHttpClient(requestTimeoutMillis = requestTimeoutMillis) }
    return GooglePlacesGeocodingApi(httpClient, GooglePlacesConfig(apiKey = "test-api-key"))
}

internal fun testGooglePlacesDetailsApi(
    requestTimeoutMillis: Long = 15_000L,
    handler: suspend MockRequestHandleScope.(request: HttpRequestData) -> HttpResponseData,
): GooglePlacesDetailsApi {
    val engine = MockEngine { request -> handler(request) }
    val httpClient = HttpClient(engine) { configureFirestoreHttpClient(requestTimeoutMillis = requestTimeoutMillis) }
    return GooglePlacesDetailsApi(httpClient, GooglePlacesConfig(apiKey = "test-api-key"))
}

internal fun testGooglePlacesPhotoApi(
    requestTimeoutMillis: Long = 15_000L,
    handler: suspend MockRequestHandleScope.(request: HttpRequestData) -> HttpResponseData,
): GooglePlacesPhotoApi {
    val engine = MockEngine { request -> handler(request) }
    val httpClient = HttpClient(engine) { configureFirestoreHttpClient(requestTimeoutMillis = requestTimeoutMillis) }
    return GooglePlacesPhotoApi(httpClient, GooglePlacesConfig(apiKey = "test-api-key"))
}

/** [followRedirects] defaults to `false` - the whole point of [KtorShareLinkRedirectResolver]. */
internal fun testKtorShareLinkRedirectResolver(
    requestTimeoutMillis: Long = 15_000L,
    followRedirects: Boolean = false,
    handler: suspend MockRequestHandleScope.(request: HttpRequestData) -> HttpResponseData,
): KtorShareLinkRedirectResolver {
    val engine = MockEngine { request -> handler(request) }
    val httpClient =
        HttpClient(engine) {
            this.followRedirects = followRedirects
            configureFirestoreHttpClient(requestTimeoutMillis = requestTimeoutMillis)
        }
    return KtorShareLinkRedirectResolver(httpClient)
}
