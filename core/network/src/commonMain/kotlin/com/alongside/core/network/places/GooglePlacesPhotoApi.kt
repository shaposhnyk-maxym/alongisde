package com.alongside.core.network.places

import com.alongside.core.network.places.model.GooglePlacesErrorResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import kotlinx.coroutines.CancellationException

/**
 * Ktor-based client for the Places API (New) photo-media endpoint - a plain GET that returns raw
 * image bytes (this client's [httpClient] follows redirects normally, unlike
 * [com.alongside.core.domain.place.importing.ShareLinkRedirectResolver]'s dedicated one).
 */
public class GooglePlacesPhotoApi(
    private val httpClient: HttpClient,
    private val config: GooglePlacesConfig,
) {
    public suspend fun fetchPhoto(photoName: String): ByteArray {
        val response = rawRequest(config.photoMediaUrl(photoName))
        throwIfError(response)
        return response.bodyAsBytes()
    }

    // Catching Exception broadly and re-throwing as a typed GooglePlacesException is the point of
    // this boundary function; CancellationException is excluded so cancellation still propagates.
    @Suppress("TooGenericExceptionCaught")
    private suspend fun rawRequest(url: String): HttpResponse =
        try {
            httpClient.get(url)
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpRequestTimeoutException) {
            println("GooglePlacesPhotoApi: request timed out before a response arrived - ${e.message}")
            throw GooglePlacesException.NetworkTimeout(e)
        } catch (e: Exception) {
            println(
                "GooglePlacesPhotoApi: request failed before a response arrived - ${e::class.simpleName}: ${e.message}",
            )
            throw GooglePlacesException.Unknown(e)
        }

    private suspend fun throwIfError(response: HttpResponse) {
        if (response.status.value in SUCCESS_RANGE) return
        val detail = errorDetailOrNull(response)
        val httpStatus = response.status.value
        val message = detail?.message ?: response.status.description
        println(
            "GooglePlacesPhotoApi: request failed - HTTP $httpStatus, status=${detail?.status}, message=$message",
        )
        throw if (httpStatus in CLIENT_ERROR_RANGE) {
            GooglePlacesException.ClientError(httpStatus, message)
        } else {
            GooglePlacesException.ServerError(httpStatus, message)
        }
    }

    // Best-effort: if the error body itself doesn't parse, throwIfError falls back to the raw
    // HTTP status description - losing this particular exception is intentional, not a bug.
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private suspend fun errorDetailOrNull(response: HttpResponse) =
        try {
            response.body<GooglePlacesErrorResponse>().error
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }

    private companion object {
        val SUCCESS_RANGE = 200..299
        val CLIENT_ERROR_RANGE = 400..499
    }
}
