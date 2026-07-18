package com.alongside.core.network.firestore

import com.alongside.core.network.firestore.model.FirestoreDocument
import com.alongside.core.network.firestore.model.FirestoreErrorResponse
import com.alongside.core.network.firestore.model.FirestoreListDocumentsResponse
import com.alongside.core.network.firestore.model.FirestoreValue
import com.alongside.core.network.firestore.model.FirestoreWriteRequest
import com.alongside.core.network.firestore.model.RunQueryRequest
import com.alongside.core.network.firestore.model.RunQueryResponseElement
import com.alongside.core.network.firestore.model.StructuredQuery
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException

/** Ktor-based client for the Firestore REST API's document endpoints. */
public class FirestoreApi(
    private val httpClient: HttpClient,
    private val config: FirestoreConfig,
    private val tokenProvider: FirestoreTokenProvider,
) {
    public suspend fun getDocument(
        collectionPath: String,
        documentId: String,
    ): FirestoreDocument {
        val response =
            rawRequest {
                method = HttpMethod.Get
                url("${config.documentsBaseUrl}/$collectionPath/$documentId")
            }
        throwIfError(response)
        return parseBody(response)
    }

    public suspend fun listDocuments(
        collectionPath: String,
        pageSize: Int? = null,
        pageToken: String? = null,
    ): FirestoreListDocumentsResponse {
        val response =
            rawRequest {
                method = HttpMethod.Get
                url("${config.documentsBaseUrl}/$collectionPath")
                pageSize?.let { parameter("pageSize", it) }
                pageToken?.let { parameter("pageToken", it) }
            }
        throwIfError(response)
        return parseBody(response)
    }

    /** Runs [structuredQuery] against the database root, returning matched documents in response order. */
    public suspend fun runQuery(structuredQuery: StructuredQuery): List<FirestoreDocument> {
        val response =
            rawRequest {
                method = HttpMethod.Post
                url("${config.documentsBaseUrl}:runQuery")
                contentType(ContentType.Application.Json)
                setBody(RunQueryRequest(structuredQuery))
            }
        throwIfError(response)
        return parseBody<List<RunQueryResponseElement>>(response).mapNotNull { it.document }
    }

    public suspend fun upsertDocument(
        collectionPath: String,
        documentId: String,
        fields: Map<String, FirestoreValue>,
    ): FirestoreDocument {
        val response =
            rawRequest {
                method = HttpMethod.Patch
                url("${config.documentsBaseUrl}/$collectionPath/$documentId")
                contentType(ContentType.Application.Json)
                setBody(FirestoreWriteRequest(fields))
            }
        throwIfError(response)
        return parseBody(response)
    }

    public suspend fun deleteDocument(
        collectionPath: String,
        documentId: String,
    ) {
        val response =
            rawRequest {
                method = HttpMethod.Delete
                url("${config.documentsBaseUrl}/$collectionPath/$documentId")
            }
        throwIfError(response)
    }

    // Catching Exception broadly and re-throwing as a typed FirestoreException is the point of
    // this boundary function; CancellationException is excluded so cancellation still propagates.
    @Suppress("TooGenericExceptionCaught", "ThrowsCount")
    private suspend fun rawRequest(block: HttpRequestBuilder.() -> Unit): HttpResponse {
        val token = tokenProvider.currentToken()
        println("FirestoreApi: token present=${token != null} length=${token?.length}")
        return try {
            httpClient.request {
                block()
                if (token != null) {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpRequestTimeoutException) {
            println("FirestoreApi: request timed out before a response arrived - ${e.message}")
            throw FirestoreException.NetworkTimeout(e)
        } catch (e: Exception) {
            // Unlike throwIfError's HTTP-level failures, this branch fires when no response was
            // ever received (DNS, TLS, missing INTERNET permission, ...) - it was previously
            // silent, which made exactly that class of failure impossible to diagnose from logs.
            println("FirestoreApi: request failed before a response arrived - ${e::class.simpleName}: ${e.message}")
            throw FirestoreException.Unknown(e)
        }
    }

    private suspend fun throwIfError(response: HttpResponse) {
        if (response.status.value in SUCCESS_RANGE) return
        val detail = errorDetailOrNull(response)
        val code = detail?.code ?: response.status.value
        val message = detail?.message ?: response.status.description
        val status = detail?.status ?: response.status.toString()
        throw if (response.status.value in CLIENT_ERROR_RANGE) {
            FirestoreException.ClientError(code, status, message)
        } else {
            FirestoreException.ServerError(code, status, message)
        }
    }

    // Best-effort: if the error body itself doesn't parse, throwIfError falls back to the raw
    // HTTP status - losing this particular exception is intentional, not a bug.
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private suspend fun errorDetailOrNull(response: HttpResponse) =
        try {
            response.body<FirestoreErrorResponse>().error
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }

    @Suppress("TooGenericExceptionCaught")
    private suspend inline fun <reified T> parseBody(response: HttpResponse): T =
        try {
            response.body()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw FirestoreException.MalformedResponse("Failed to parse Firestore response body", e)
        }

    private companion object {
        val SUCCESS_RANGE = 200..299
        val CLIENT_ERROR_RANGE = 400..499
    }
}
