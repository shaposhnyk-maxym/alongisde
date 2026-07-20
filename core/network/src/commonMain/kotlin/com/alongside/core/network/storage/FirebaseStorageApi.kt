package com.alongside.core.network.storage

import com.alongside.core.network.firestore.FirestoreTokenProvider
import com.alongside.core.network.firestore.model.firestoreJson
import com.alongside.core.network.storage.model.FirebaseStorageErrorResponse
import com.alongside.core.network.storage.model.FirebaseStorageUploadResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.decodeFromString

/**
 * Ktor-based client for the Firebase Storage REST API's object-upload endpoint. Stays a dumb
 * parse/transport layer - interpreting the response (building the final download URL) is
 * [FirebaseStorageUploadClient]'s job, mirroring the [com.alongside.core.network.places.GooglePlacesGeocodingApi] /
 * [com.alongside.core.network.places.GooglePlacesGeocodingClient] split.
 */
public class FirebaseStorageApi(
    private val httpClient: HttpClient,
    private val config: FirebaseStorageConfig,
    private val tokenProvider: FirestoreTokenProvider,
) {
    public suspend fun upload(
        photoId: String,
        bytes: ByteArray,
        contentType: String = "image/jpeg",
    ): FirebaseStorageUploadResponse {
        val response =
            rawRequest(config.uploadUrl(photoId)) {
                contentType(ContentType.parse(contentType))
                setBody(bytes)
            }
        throwIfError(response)
        return parseBody(response)
    }

    // Catching Exception broadly and re-throwing as a typed FirebaseStorageException is the point
    // of this boundary function; CancellationException is excluded so cancellation still propagates.
    @Suppress("TooGenericExceptionCaught", "ThrowsCount")
    private suspend fun rawRequest(
        url: String,
        block: HttpRequestBuilder.() -> Unit,
    ): HttpResponse {
        val token = tokenProvider.currentToken()
        return try {
            httpClient.post(url) {
                block()
                // Firebase Storage's REST auth scheme is the literal string "Firebase", not
                // "Bearer" like Firestore/Auth - distinct from every other client in this module.
                if (token != null) {
                    header(HttpHeaders.Authorization, "Firebase $token")
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpRequestTimeoutException) {
            println("FirebaseStorageApi: request timed out before a response arrived - ${e.message}")
            throw FirebaseStorageException.NetworkTimeout(e)
        } catch (e: Exception) {
            println(
                "FirebaseStorageApi: request failed before a response arrived - ${e::class.simpleName}: ${e.message}",
            )
            throw FirebaseStorageException.Unknown(e)
        }
    }

    private suspend fun throwIfError(response: HttpResponse) {
        if (response.status.value in SUCCESS_RANGE) return
        val detail = errorDetailOrNull(response)
        val httpStatus = response.status.value
        val message = detail?.message ?: response.status.description
        throw if (httpStatus in CLIENT_ERROR_RANGE) {
            FirebaseStorageException.ClientError(httpStatus, message)
        } else {
            FirebaseStorageException.ServerError(httpStatus, message)
        }
    }

    // Best-effort: if the error body itself doesn't parse, throwIfError falls back to the raw
    // HTTP status description - losing this particular exception is intentional, not a bug.
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private suspend fun errorDetailOrNull(response: HttpResponse) =
        try {
            response.body<FirebaseStorageErrorResponse>().error
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }

    @Suppress("TooGenericExceptionCaught")
    private suspend inline fun <reified T> parseBody(response: HttpResponse): T {
        val rawBody = response.bodyAsText()
        return try {
            firestoreJson.decodeFromString(rawBody)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw FirebaseStorageException.MalformedResponse(
                "Failed to parse Firebase Storage response body: $rawBody",
                e,
            )
        }
    }

    private companion object {
        val SUCCESS_RANGE = 200..299
        val CLIENT_ERROR_RANGE = 400..499
    }
}
