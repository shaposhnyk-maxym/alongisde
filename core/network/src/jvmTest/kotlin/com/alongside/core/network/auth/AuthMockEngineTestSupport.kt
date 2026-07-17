package com.alongside.core.network.auth

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

internal fun testFirebaseAuthApi(
    requestTimeoutMillis: Long = 15_000L,
    handler: suspend MockRequestHandleScope.(request: HttpRequestData) -> HttpResponseData,
): FirebaseAuthApi {
    val engine = MockEngine { request -> handler(request) }
    val httpClient = HttpClient(engine) { configureFirestoreHttpClient(requestTimeoutMillis = requestTimeoutMillis) }
    return FirebaseAuthApi(httpClient, FirebaseAuthConfig(apiKey = "test-api-key"))
}
