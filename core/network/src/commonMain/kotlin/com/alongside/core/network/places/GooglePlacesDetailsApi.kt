package com.alongside.core.network.places

import com.alongside.core.network.firestore.model.firestoreJson
import com.alongside.core.network.places.model.GooglePlacesErrorResponse
import com.alongside.core.network.places.model.PlaceSearchTextRequest
import com.alongside.core.network.places.model.PlaceSearchTextResponse
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
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.decodeFromString

/**
 * Ktor-based client for the Places API (New) `places:searchText` endpoint - a single call that
 * returns name, rating, category (`primaryTypeDisplayName`) and photo references together,
 * unlike the classic Find-Place-from-Text + Details two-call chain. Auth is a request header
 * (`X-Goog-Api-Key`), not a query-string key like [GooglePlacesGeocodingApi] - a different Google
 * API generation, same [GooglePlacesConfig] api key.
 */
public class GooglePlacesDetailsApi(
    private val httpClient: HttpClient,
    private val config: GooglePlacesConfig,
) {
    public suspend fun searchText(query: String): PlaceSearchTextResponse {
        val response =
            rawRequest(GooglePlacesConfig.SEARCH_TEXT_URL) {
                header(FIELD_MASK_HEADER, FIELD_MASK)
                contentType(ContentType.Application.Json)
                setBody(PlaceSearchTextRequest(textQuery = query))
            }
        throwIfError(response)
        return parseBody(response)
    }

    // Catching Exception broadly and re-throwing as a typed GooglePlacesException is the point of
    // this boundary function; CancellationException is excluded so cancellation still propagates.
    @Suppress("TooGenericExceptionCaught")
    private suspend fun rawRequest(
        url: String,
        block: HttpRequestBuilder.() -> Unit,
    ): HttpResponse =
        try {
            httpClient.post(url) {
                header(API_KEY_HEADER, config.apiKey)
                block()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpRequestTimeoutException) {
            println("GooglePlacesDetailsApi: request timed out before a response arrived - ${e.message}")
            throw GooglePlacesException.NetworkTimeout(e)
        } catch (e: Exception) {
            println(
                "GooglePlacesDetailsApi: request failed before a response arrived - " +
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
            "GooglePlacesDetailsApi: request failed - HTTP $httpStatus, status=${detail?.status}, message=$message",
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
            println("GooglePlacesDetailsApi: failed to parse response body: $rawBody")
            throw GooglePlacesException.MalformedResponse("Failed to parse Places response body: $rawBody", e)
        }
    }

    private companion object {
        const val API_KEY_HEADER = "X-Goog-Api-Key"
        const val FIELD_MASK_HEADER = "X-Goog-FieldMask"
        const val FIELD_MASK =
            "places.displayName,places.rating,places.primaryTypeDisplayName,places.photos,places.location"
        val SUCCESS_RANGE = 200..299
        val CLIENT_ERROR_RANGE = 400..499
    }
}
