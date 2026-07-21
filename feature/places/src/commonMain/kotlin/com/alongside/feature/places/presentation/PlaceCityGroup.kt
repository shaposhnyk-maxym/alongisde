package com.alongside.feature.places.presentation

import com.alongside.core.model.place.PlaceCandidate

/** [city] is null for the trailing "no city known yet" group - rendered as "Other" by the screen. */
public data class PlaceCityGroup(
    val city: String?,
    val places: List<PlaceCandidate>,
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
            .map { (city, places) -> PlaceCityGroup(city, places) }
    return if (unnamed.isEmpty()) namedGroups else namedGroups + PlaceCityGroup(city = null, places = unnamed)
}
