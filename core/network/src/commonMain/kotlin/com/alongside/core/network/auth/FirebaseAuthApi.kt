package com.alongside.core.network.auth

import com.alongside.core.network.auth.model.FirebaseAuthErrorResponse
import com.alongside.core.network.auth.model.FirebaseSignInRequest
import com.alongside.core.network.auth.model.FirebaseSignInResponse
import com.alongside.core.network.firestore.model.firestoreJson
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.decodeFromString

/** Ktor-based client for Identity Toolkit's `accounts:signInWithIdp` REST endpoint. */
public class FirebaseAuthApi(
    private val httpClient: HttpClient,
    private val config: FirebaseAuthConfig,
) {
    public suspend fun signInWithGoogleIdToken(googleIdToken: String): FirebaseSignInResponse {
        val response =
            rawRequest {
                contentType(ContentType.Application.Json)
                setBody(FirebaseSignInRequest.forGoogleIdToken(googleIdToken))
            }
        throwIfError(response)
        return parseBody(response)
    }

    // Catching Exception broadly and re-throwing as a typed FirebaseAuthException is the point of
    // this boundary function; CancellationException is excluded so cancellation still propagates.
    @Suppress("TooGenericExceptionCaught")
    private suspend fun rawRequest(block: HttpRequestBuilder.() -> Unit): HttpResponse =
        try {
            httpClient.post(config.signInWithIdpUrl, block)
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpRequestTimeoutException) {
            throw FirebaseAuthException.NetworkTimeout(e)
        } catch (e: Exception) {
            throw FirebaseAuthException.Unknown(e)
        }

    private suspend fun throwIfError(response: HttpResponse) {
        if (response.status.value in SUCCESS_RANGE) return
        val detail = errorDetailOrNull(response)
        val code = detail?.code ?: response.status.value
        val message = detail?.message ?: response.status.description
        val status = detail?.status ?: response.status.toString()
        println(
            "FirebaseAuthApi: signInWithIdp failed - HTTP ${response.status.value}, " +
                "code=$code, status=$status, message=$message",
        )
        throw when {
            response.status.value in CLIENT_ERROR_RANGE && isInvalidTokenMessage(message) ->
                FirebaseAuthException.InvalidIdToken(code, status, message)
            response.status.value in CLIENT_ERROR_RANGE -> FirebaseAuthException.ClientError(code, status, message)
            else -> FirebaseAuthException.ServerError(code, status, message)
        }
    }

    private fun isInvalidTokenMessage(message: String): Boolean = INVALID_TOKEN_MESSAGES.any(message::startsWith)

    // Best-effort: if the error body itself doesn't parse, throwIfError falls back to the raw
    // HTTP status - losing this particular exception is intentional, not a bug.
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private suspend fun errorDetailOrNull(response: HttpResponse) =
        try {
            response.body<FirebaseAuthErrorResponse>().error
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }

    // Reads the raw text first (rather than response.body<T>()) so the actual payload is always
    // available to log on a parse failure - Identity Toolkit's signInWithIdp endpoint has been
    // known to return HTTP 200 with an error embedded in the body for some IdP-verification
    // failures, which looks nothing like FirebaseSignInResponse and needs to be seen to diagnose.
    @Suppress("TooGenericExceptionCaught")
    private suspend inline fun <reified T> parseBody(response: HttpResponse): T {
        val rawBody = response.bodyAsText()
        return try {
            firestoreJson.decodeFromString(rawBody)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            println("FirebaseAuthApi: failed to parse response body: $rawBody")
            throw FirebaseAuthException.MalformedResponse("Failed to parse Firebase Auth response body: $rawBody", e)
        }
    }

    private companion object {
        val SUCCESS_RANGE = 200..299
        val CLIENT_ERROR_RANGE = 400..499
        val INVALID_TOKEN_MESSAGES = listOf("INVALID_IDP_RESPONSE", "INVALID_ID_TOKEN")
    }
}
