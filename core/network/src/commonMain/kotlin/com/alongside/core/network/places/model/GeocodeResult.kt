package com.alongside.core.network.places.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class GeocodeResult(
    @SerialName("formatted_address") val formattedAddress: String,
    @SerialName("address_components") val addressComponents: List<AddressComponent> = emptyList(),
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
