package com.alongside.core.domain.place.importing

/**
 * Looks up a Google Place's rich details (rating, category, photo references) by name/address/
 * approximate coordinates - i.e. exactly what [GoogleMapsShareLinkParser] extracts from a
 * resolved share link. Same shape as
 * [com.alongside.core.domain.diary.processing.PlaceGeocodingClient]: a narrow interface in
 * core:domain, with the real Google Places-backed implementation living in core:network.
 */
public interface PlaceDetailsLookupClient {
    public suspend fun lookup(query: PlaceLookupQuery): PlaceDetailsResult
}

public data class PlaceLookupQuery(
    public val name: String,
    public val address: String?,
    public val latitude: Double?,
    public val longitude: Double?,
)

public sealed class PlaceDetailsResult {
    public data class Found(
        public val name: String,
        public val rating: Double?,
        public val category: String?,
        public val photoRefs: List<String>,
        public val latitude: Double,
        public val longitude: Double,
    ) : PlaceDetailsResult()

    public data object NotFound : PlaceDetailsResult()

    public data class Failure(
        public val cause: Throwable,
    ) : PlaceDetailsResult()
}
