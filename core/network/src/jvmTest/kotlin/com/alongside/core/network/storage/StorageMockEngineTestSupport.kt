package com.alongside.core.network.storage

import com.alongside.core.network.client.configureFirestoreHttpClient
import com.alongside.core.network.firestore.FirestoreTokenProvider
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

internal class FakeStorageTokenProvider(
    private val token: String? = "test-id-token",
) : FirestoreTokenProvider {
    override suspend fun currentToken(): String? = token
}

internal fun testFirebaseStorageApi(
    requestTimeoutMillis: Long = 15_000L,
    tokenProvider: FirestoreTokenProvider = FakeStorageTokenProvider(),
    handler: suspend MockRequestHandleScope.(request: HttpRequestData) -> HttpResponseData,
): FirebaseStorageApi {
    val engine = MockEngine { request -> handler(request) }
    val httpClient = HttpClient(engine) { configureFirestoreHttpClient(requestTimeoutMillis = requestTimeoutMillis) }
    val config = FirebaseStorageConfig(bucket = "test-bucket.firebasestorage.app")
    return FirebaseStorageApi(httpClient, config, tokenProvider)
}
