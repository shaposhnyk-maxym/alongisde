package com.alongside.core.domain.diary.processing

/**
 * Reverse-geocoding seam for episode locations. Same shape as
 * [com.alongside.core.domain.pairing.PairingTripDataSource]: a narrow interface in core:domain,
 * with the real Google Places-backed implementation living in core:network so this stays testable
 * against fakes, independent of Ktor.
 */
public interface PlaceGeocodingClient {
    public suspend fun reverseGeocode(
        latitude: Double,
        longitude: Double,
    ): GeocodingResult
}
