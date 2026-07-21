package com.alongside.core.network.places

import com.alongside.core.domain.place.importing.PlacePhotoClient
import kotlinx.coroutines.CancellationException

/** Adapts [GooglePlacesPhotoApi] to the [PlacePhotoClient] seam core:domain depends on. */
public class GooglePlacesPhotoClient(
    private val api: GooglePlacesPhotoApi,
) : PlacePhotoClient {
    override suspend fun fetchPhotoBytes(photoRef: String): ByteArray? =
        try {
            api.fetchPhoto(photoRef)
        } catch (e: CancellationException) {
            throw e
        } catch (e: GooglePlacesException) {
            println("GooglePlacesPhotoClient: failed to fetch $photoRef - ${e::class.simpleName}: ${e.message}")
            null
        }
}
