package com.alongside.core.network.places.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class GeocodeResult(
    @SerialName("formatted_address") val formattedAddress: String,
    @SerialName("address_components") val addressComponents: List<AddressComponent> = emptyList(),
    @SerialName("place_id") val placeId: String? = null,
    val types: List<String> = emptyList(),
)

/**
 * Prefers a human-scale place name over the full formatted address - falls back to
 * [GeocodeResult.formattedAddress] when nothing more specific is tagged.
 *
 * `route`/`neighborhood` sit ahead of `locality` on purpose: verified against a real Google
 * Geocoding response (M10 manual test) that two episodes ~4km apart in the same city both fell
 * through to the bare city name (`locality`) because neither address had a `point_of_interest`/
 * `premise` component - a real reverse-geocoding response rarely tags an individual address
 * component as `point_of_interest` even when the result *as a whole* is one. Preferring the
 * street/neighborhood name keeps same-city episodes visually distinct in the diary.
 */
public fun GeocodeResult.preferredPlaceName(): String {
    for (type in PREFERRED_COMPONENT_TYPES) {
        val match = addressComponents.firstOrNull { type in it.types }
        if (match != null) return match.longName
    }
    return formattedAddress
}

private val PREFERRED_COMPONENT_TYPES =
    listOf(
        "point_of_interest",
        "premise",
        "neighborhood",
        "sublocality_level_1",
        "route",
        "locality",
        "administrative_area_level_2",
    )

/**
 * Unlike [preferredPlaceName], which deliberately prefers a street/POI name over the bare city,
 * this wants specifically the city - `locality` first, falling back to the next-coarsest
 * administrative levels for addresses without one tagged. Null (not the formatted address) when
 * nothing in this cascade matches - a place's "city" grouping is allowed to be absent, unlike a
 * display name which always needs something to show.
 */
public fun GeocodeResult.cityName(): String? {
    for (type in CITY_COMPONENT_TYPES) {
        val match = addressComponents.firstOrNull { type in it.types }
        if (match != null) return match.longName
    }
    return null
}

private val CITY_COMPONENT_TYPES =
    listOf(
        "locality",
        "administrative_area_level_2",
        "administrative_area_level_1",
    )

/** ISO 3166-1 alpha-2 code from the `country`-typed address component's `short_name`, if tagged. */
public fun GeocodeResult.countryCode(): String? = addressComponents.firstOrNull { "country" in it.types }?.shortName

/**
 * Unlike [countryCode]/[cityName], which read a single result's `address_components`, this reads
 * the top-level `place_id` of whichever result in the full response is itself typed `locality` -
 * Google's reverse-geocoding response is a list of same-coordinate interpretations at different
 * granularities (street address, locality, administrative area, country, ...), each with its own
 * `place_id`. That `place_id` is a stable, already-available identifier for the city itself -
 * useful for later re-localizing its display name without a new geocoding request.
 */
public fun List<GeocodeResult>.localityPlaceId(): String? = firstOrNull { "locality" in it.types }?.placeId
