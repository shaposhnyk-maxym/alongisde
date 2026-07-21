package com.alongside.core.network.places

import com.alongside.core.domain.place.importing.PlaceDetailsLookupClient
import com.alongside.core.domain.place.importing.PlaceDetailsResult
import com.alongside.core.domain.place.importing.PlaceLookupQuery
import com.alongside.core.network.places.model.GooglePlaceResult
import kotlinx.coroutines.CancellationException

/** Adapts [GooglePlacesDetailsApi] to the [PlaceDetailsLookupClient] seam core:domain depends on. */
public class GooglePlacesDetailsClient(
    private val api: GooglePlacesDetailsApi,
) : PlaceDetailsLookupClient {
    override suspend fun lookup(query: PlaceLookupQuery): PlaceDetailsResult =
        try {
            val textQuery = listOfNotNull(query.name, query.address).joinToString(", ")
            val place = api.searchText(textQuery).places.firstOrNull()
            place?.toPlaceDetailsResult(query) ?: PlaceDetailsResult.NotFound
        } catch (e: CancellationException) {
            throw e
        } catch (e: GooglePlacesException) {
            PlaceDetailsResult.Failure(e)
        }

    // Falls back to the query's own coordinates (the parsed share link's, when present) if the
    // API result carries none - only genuinely missing both counts as NotFound.
    private fun GooglePlaceResult.toPlaceDetailsResult(query: PlaceLookupQuery): PlaceDetailsResult? {
        val latitude = location?.latitude ?: query.latitude
        val longitude = location?.longitude ?: query.longitude
        if (latitude == null || longitude == null) return null
        return PlaceDetailsResult.Found(
            name = displayName?.text ?: query.name,
            rating = rating,
            category = primaryTypeDisplayName?.text,
            photoRefs = photos.map { it.name },
            latitude = latitude,
            longitude = longitude,
        )
    }
}
