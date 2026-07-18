package com.alongside.core.network.places

import com.alongside.core.network.firestore.model.firestoreJson
import com.alongside.core.network.places.model.GeocodeResponse
import com.alongside.core.network.places.model.GooglePlacesErrorResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.decodeFromString

/** Ktor-based client for the Google Maps Geocoding API's reverse-geocoding lookup. */
public class GooglePlacesGeocodingApi(
    private val httpClient: HttpClient,
    private val config: GooglePlacesConfig,
) {
    public suspend fun reverseGeocode(
        latitude: Double,
        longitude: Double,
    ): GeocodeResponse {
        val response = rawRequest(config.reverseGeocodeUrl(latitude, longitude))
        throwIfError(response)
        return parseBody(response)
    }

    // Catching Exception broadly and re-throwing as a typed GooglePlacesException is the point of
    // this boundary function; CancellationException is excluded so cancellation still propagates.
    @Suppress("TooGenericExceptionCaught")
    private suspend fun rawRequest(
        url: String,
        block: HttpRequestBuilder.() -> Unit = {},
    ): HttpResponse =
        try {
            httpClient.get(url, block)
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpRequestTimeoutException) {
            println("GooglePlacesGeocodingApi: request timed out before a response arrived - ${e.message}")
            throw GooglePlacesException.NetworkTimeout(e)
        } catch (e: Exception) {
            println(
                "GooglePlacesGeocodingApi: request failed before a response arrived - " +
                    "${e::class.simpleName}: ${e.message}",
            )
            throw GooglePlacesException.Unknown(e)
        }

    private suspend fun throwIfError(response: HttpResponse) {
        if (response.status.value in SUCCESS_RANGE) return
        val detail = errorDetailOrNull(response)
        val httpStatus = response.status.value
        val message = detail?.message ?: response.status.description
        println(
            "GooglePlacesGeocodingApi: request failed - HTTP $httpStatus, status=${detail?.status}, message=$message",
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

    @Suppress("TooGenericExceptionCaught")
    private suspend inline fun <reified T> parseBody(response: HttpResponse): T {
        val rawBody = response.bodyAsText()
        return try {
            firestoreJson.decodeFromString(rawBody)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            println("GooglePlacesGeocodingApi: failed to parse response body: $rawBody")
            throw GooglePlacesException.MalformedResponse("Failed to parse Google Places response body: $rawBody", e)
        }
    }

    private companion object {
        val SUCCESS_RANGE = 200..299
        val CLIENT_ERROR_RANGE = 400..499
    }
}
