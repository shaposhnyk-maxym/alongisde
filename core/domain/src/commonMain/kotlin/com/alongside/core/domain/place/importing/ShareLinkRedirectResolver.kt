package com.alongside.core.domain.place.importing

/**
 * Follows a `maps.app.goo.gl` short link's HTTP redirect and returns the final, resolved URL -
 * the raw text [GoogleMapsShareLinkParser] then parses. Same shape as
 * [com.alongside.core.domain.diary.processing.PlaceGeocodingClient]: a narrow interface in
 * core:domain, with the real Ktor-backed implementation living in core:network so this stays
 * testable against fakes.
 */
public interface ShareLinkRedirectResolver {
    public suspend fun resolve(shortUrl: String): ShareLinkRedirectResult
}

public sealed class ShareLinkRedirectResult {
    public data class Resolved(
        public val url: String,
    ) : ShareLinkRedirectResult()

    public data class Failure(
        public val cause: Throwable,
    ) : ShareLinkRedirectResult()
}
