package com.alongside.core.network.places

import com.alongside.core.domain.diary.processing.GeocodingResult
import com.alongside.core.domain.diary.processing.PlaceGeocodingClient
import com.alongside.core.network.places.model.GeocodeResponse
import com.alongside.core.network.places.model.cityName
import com.alongside.core.network.places.model.preferredPlaceName
import kotlinx.coroutines.CancellationException

/** Adapts [GooglePlacesGeocodingApi] to the [PlaceGeocodingClient] seam core:domain depends on. */
public class GooglePlacesGeocodingClient(
    private val api: GooglePlacesGeocodingApi,
) : PlaceGeocodingClient {
    override suspend fun reverseGeocode(
        latitude: Double,
        longitude: Double,
    ): GeocodingResult =
        try {
            api.reverseGeocode(latitude, longitude).toGeocodingResult()
        } catch (e: CancellationException) {
            throw e
        } catch (e: GooglePlacesException) {
            GeocodingResult.Failure(e)
        }

    private fun GeocodeResponse.toGeocodingResult(): GeocodingResult =
        when (status) {
            "OK" -> {
                val firstResult = results.firstOrNull()
                if (firstResult != null) {
                    GeocodingResult.Found(placeName = firstResult.preferredPlaceName(), city = firstResult.cityName())
                } else {
                    GeocodingResult.NotFound
                }
            }
            "ZERO_RESULTS" -> GeocodingResult.NotFound
            else -> GeocodingResult.Failure(GooglePlacesException.ApiStatus(status, errorMessage))
        }
}
