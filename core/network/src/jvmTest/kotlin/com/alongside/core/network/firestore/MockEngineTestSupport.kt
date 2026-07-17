package com.alongside.core.network.firestore

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

internal fun testFirestoreApi(
    requestTimeoutMillis: Long = 15_000L,
    tokenProvider: FirestoreTokenProvider = FirestoreTokenProvider { null },
    handler: suspend MockRequestHandleScope.(request: HttpRequestData) -> HttpResponseData,
): FirestoreApi {
    val engine = MockEngine { request -> handler(request) }
    val httpClient = HttpClient(engine) { configureFirestoreHttpClient(requestTimeoutMillis = requestTimeoutMillis) }
    return FirestoreApi(httpClient, FirestoreConfig(projectId = "alongside-test"), tokenProvider)
}
