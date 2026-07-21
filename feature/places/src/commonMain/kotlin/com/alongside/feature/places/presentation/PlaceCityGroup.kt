package com.alongside.feature.places.presentation

import com.alongside.core.model.place.PlaceCandidate

/**
 * [city] is null for the trailing "no city known yet" group - rendered as "Other" by the screen.
 * [countryCode] is the shared ISO 3166-1 alpha-2 code of the group's places (they're grouped by
 * the same city, so in practice they share a country) - null for the trailing group, same as
 * [city].
 */
public data class PlaceCityGroup(
    val city: String?,
    val places: List<PlaceCandidate>,
    val countryCode: String? = null,
)

/**
 * Named cities first, alphabetically; places with no `city` (reverse-geocoding never ran, failed,
 * or found nothing - see `PlaceImportPipeline.lookupCity`) collected into one trailing group
 * rather than dropped - same "omit gracefully, don't fail" convention as a missing rating/category.
 */
public fun List<PlaceCandidate>.groupedByCity(): List<PlaceCityGroup> {
    val (named, unnamed) = partition { it.city != null }
    val namedGroups =
        named
            .groupBy { requireNotNull(it.city) }
            .entries
            .sortedBy { it.key }
            .map { (city, places) -> PlaceCityGroup(city, places, countryCode = places.first().countryCode) }
    return if (unnamed.isEmpty()) namedGroups else namedGroups + PlaceCityGroup(city = null, places = unnamed)
}
