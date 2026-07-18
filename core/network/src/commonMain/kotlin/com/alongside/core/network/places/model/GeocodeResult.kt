package com.alongside.core.network.places.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class GeocodeResult(
    @SerialName("formatted_address") val formattedAddress: String,
    @SerialName("address_components") val addressComponents: List<AddressComponent> = emptyList(),
)

/**
 * Prefers a human-scale place name (point of interest, locality) over the full formatted
 * address - falls back to [GeocodeResult.formattedAddress] when nothing more specific is tagged.
 */
public fun GeocodeResult.preferredPlaceName(): String {
    for (type in PREFERRED_COMPONENT_TYPES) {
        val match = addressComponents.firstOrNull { type in it.types }
        if (match != null) return match.longName
    }
    return formattedAddress
}

private val PREFERRED_COMPONENT_TYPES =
    listOf("point_of_interest", "premise", "sublocality_level_1", "locality", "administrative_area_level_2")
