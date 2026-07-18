package com.alongside.core.network.places

import com.alongside.core.network.firestore.model.firestoreJson
import com.alongside.core.network.places.model.GeocodeResponse
import io.ktor.client.HttpClient
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

    private fun throwIfError(response: HttpResponse) {
        if (response.status.value in SUCCESS_RANGE) return
        println(
            "GooglePlacesGeocodingApi: request failed - HTTP ${response.status.value} ${response.status.description}",
        )
        throw if (response.status.value in CLIENT_ERROR_RANGE) {
            GooglePlacesException.ClientError(response.status.value, response.status.description)
        } else {
            GooglePlacesException.ServerError(response.status.value, response.status.description)
        }
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
