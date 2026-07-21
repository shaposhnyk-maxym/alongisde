package com.alongside.feature.places

import com.alongside.core.domain.diary.processing.GeocodingResult
import com.alongside.core.domain.diary.processing.PlaceGeocodingClient
import com.alongside.core.domain.place.importing.PlaceDetailsLookupClient
import com.alongside.core.domain.place.importing.PlaceDetailsResult
import com.alongside.core.domain.place.importing.PlaceLookupQuery
import com.alongside.core.domain.place.importing.PlacePhotoClient
import com.alongside.core.domain.place.importing.PlacePhotoUploadClient
import com.alongside.core.domain.place.importing.PlacePhotoUploadResult
import com.alongside.core.domain.place.importing.ShareLinkRedirectResolver
import com.alongside.core.domain.place.importing.ShareLinkRedirectResult

/**
 * `feature:places`-local fakes for [com.alongside.core.domain.place.importing.PlaceImportPipeline]'s
 * five seams - duplicated rather than shared with `core:domain`'s own `PlaceImportPipelineTest`
 * fixtures, same convention `feature:diary`'s own `Fake*.kt` set already follows for
 * `EpisodeProcessingPipeline`'s seams.
 */
internal class FakeShareLinkRedirectResolver(
    private val result: ShareLinkRedirectResult,
) : ShareLinkRedirectResolver {
    override suspend fun resolve(shortUrl: String): ShareLinkRedirectResult = result
}

internal class FakePlaceDetailsLookupClient(
    private val result: PlaceDetailsResult,
) : PlaceDetailsLookupClient {
    override suspend fun lookup(query: PlaceLookupQuery): PlaceDetailsResult = result
}

internal class FakePlacePhotoClient(
    private val bytesByRef: Map<String, ByteArray?> = emptyMap(),
) : PlacePhotoClient {
    override suspend fun fetchPhotoBytes(photoRef: String): ByteArray? = bytesByRef[photoRef]
}

internal class FakePlacePhotoUploadClient : PlacePhotoUploadClient {
    override suspend fun upload(
        placeCandidateId: String,
        photoIndex: Int,
        bytes: ByteArray,
    ): PlacePhotoUploadResult {
        val url = "https://storage/place-photos/$placeCandidateId/$photoIndex"
        return PlacePhotoUploadResult.Uploaded(url)
    }
}

internal class FakePlaceGeocodingClient(
    private val result: GeocodingResult = GeocodingResult.Found(placeName = "Lviv Coffee Manufacture", city = "Lviv"),
) : PlaceGeocodingClient {
    override suspend fun reverseGeocode(
        latitude: Double,
        longitude: Double,
    ): GeocodingResult = result
}
