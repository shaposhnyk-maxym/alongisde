package com.alongside.core.network.gemini

import com.alongside.core.network.firestore.model.firestoreJson
import com.alongside.core.network.gemini.model.GeminiErrorResponse
import com.alongside.core.network.gemini.model.GenerateContentRequest
import com.alongside.core.network.gemini.model.GenerateContentResponse
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

/** Ktor-based client for the Gemini `generateContent` REST endpoint (multimodal: text + inline-base64 images). */
public class GeminiVisionApi(
    private val httpClient: HttpClient,
    private val config: GeminiConfig,
) {
    public suspend fun generateContent(request: GenerateContentRequest): GenerateContentResponse {
        val response =
            rawRequest(config.generateContentUrl) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        throwIfError(response)
        return parseBody(response)
    }

    // Catching Exception broadly and re-throwing as a typed GeminiException is the point of this
    // boundary function; CancellationException is excluded so cancellation still propagates.
    @Suppress("TooGenericExceptionCaught")
    private suspend fun rawRequest(
        url: String,
        block: HttpRequestBuilder.() -> Unit,
    ): HttpResponse =
        try {
            httpClient.post(url, block)
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpRequestTimeoutException) {
            println("GeminiVisionApi: request timed out before a response arrived - ${e.message}")
            throw GeminiException.NetworkTimeout(e)
        } catch (e: Exception) {
            println("GeminiVisionApi: request failed before a response arrived - ${e::class.simpleName}: ${e.message}")
            throw GeminiException.Unknown(e)
        }

    private suspend fun throwIfError(response: HttpResponse) {
        if (response.status.value in SUCCESS_RANGE) return
        val detail = errorDetailOrNull(response)
        val httpStatus = response.status.value
        val message = detail?.message ?: response.status.description
        println("GeminiVisionApi: request failed - HTTP $httpStatus, status=${detail?.status}, message=$message")
        throw if (httpStatus in CLIENT_ERROR_RANGE) {
            GeminiException.ClientError(httpStatus, message)
        } else {
            GeminiException.ServerError(httpStatus, message)
        }
    }

    // Best-effort: if the error body itself doesn't parse, throwIfError falls back to the raw
    // HTTP status description - losing this particular exception is intentional, not a bug.
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private suspend fun errorDetailOrNull(response: HttpResponse) =
        try {
            response.body<GeminiErrorResponse>().error
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
            println("GeminiVisionApi: failed to parse response body: $rawBody")
            throw GeminiException.MalformedResponse("Failed to parse Gemini response body: $rawBody", e)
        }
    }

    private companion object {
        val SUCCESS_RANGE = 200..299
        val CLIENT_ERROR_RANGE = 400..499
    }
}
