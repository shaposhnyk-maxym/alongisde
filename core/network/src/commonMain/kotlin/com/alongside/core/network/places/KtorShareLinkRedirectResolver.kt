package com.alongside.core.network.places

import com.alongside.core.domain.place.importing.ShareLinkRedirectResolver
import com.alongside.core.domain.place.importing.ShareLinkRedirectResult
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.CancellationException

/**
 * Resolves a `maps.app.goo.gl` short link's redirect target without following it. [httpClient]
 * **must** be built with `followRedirects = false` (a client-level Ktor
 * [io.ktor.client.HttpClientConfig] setting - there is no per-request override) - a client with
 * the default `true` would silently hand back the fully-followed final response instead, which
 * has no `Location` header to read.
 */
public class KtorShareLinkRedirectResolver(
    private val httpClient: HttpClient,
) : ShareLinkRedirectResolver {
    @Suppress("TooGenericExceptionCaught")
    override suspend fun resolve(shortUrl: String): ShareLinkRedirectResult =
        try {
            val response = httpClient.get(shortUrl)
            val location = response.headers[HttpHeaders.Location]
            if (response.status.value in REDIRECT_RANGE && location != null) {
                ShareLinkRedirectResult.Resolved(location)
            } else {
                ShareLinkRedirectResult.Failure(
                    IllegalStateException(
                        "Expected a redirect with a Location header, got HTTP ${response.status.value}",
                    ),
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ShareLinkRedirectResult.Failure(e)
        }

    private companion object {
        val REDIRECT_RANGE = 300..399
    }
}
