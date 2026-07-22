package com.alongside.core.domain.place.importing

import com.alongside.core.domain.diary.processing.GeocodingResult
import com.alongside.core.domain.diary.processing.PlaceGeocodingClient
import com.alongside.core.model.SyncStatus
import com.alongside.core.model.place.PlaceCandidate
import com.alongside.core.model.place.PlacePhoto
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Clock
import kotlin.time.Instant

private val FIXED_NOW = Instant.fromEpochMilliseconds(1_752_700_000_000)

private object FixedClock : Clock {
    override fun now(): Instant = FIXED_NOW
}

/** Fake [ShareLinkRedirectResolver] - scriptable, records what it was asked to resolve. */
private class FakeShareLinkRedirectResolver(
    private val result: ShareLinkRedirectResult,
) : ShareLinkRedirectResolver {
    var lastShortUrl: String? = null

    override suspend fun resolve(shortUrl: String): ShareLinkRedirectResult {
        lastShortUrl = shortUrl
        return result
    }
}

/** Fake [PlaceDetailsLookupClient] - scriptable, records the last query it received. */
private class FakePlaceDetailsLookupClient(
    private val result: PlaceDetailsResult,
) : PlaceDetailsLookupClient {
    var lastQuery: PlaceLookupQuery? = null

    override suspend fun lookup(query: PlaceLookupQuery): PlaceDetailsResult {
        lastQuery = query
        return result
    }
}

/** Fake [PlacePhotoClient] - returns bytes keyed by photoRef, `null` simulates a fetch failure. */
private class FakePlacePhotoClient(
    private val bytesByRef: Map<String, ByteArray?>,
) : PlacePhotoClient {
    override suspend fun fetchPhotoBytes(photoRef: String): ByteArray? = bytesByRef[photoRef]
}

/** Fake [PlacePhotoUploadClient] - uploads deterministically unless [failIndices] says otherwise. */
private class FakePlacePhotoUploadClient(
    private val failIndices: Set<Int> = emptySet(),
) : PlacePhotoUploadClient {
    override suspend fun upload(
        placeCandidateId: String,
        photoIndex: Int,
        bytes: ByteArray,
    ): PlacePhotoUploadResult =
        if (photoIndex in failIndices) {
            PlacePhotoUploadResult.Failure(IllegalStateException("simulated upload failure"))
        } else {
            PlacePhotoUploadResult.Uploaded("https://storage/place-photos/$placeCandidateId/$photoIndex")
        }
}

/** Fake [PlaceGeocodingClient] - scriptable, defaults to a found city. */
private class FakePlaceGeocodingClient(
    private val result: GeocodingResult =
        GeocodingResult.Found(
            placeName = "Lviv Coffee Manufacture",
            city = "Lviv",
            cityPlaceId = "locality-place-id",
            countryCode = "UA",
        ),
) : PlaceGeocodingClient {
    override suspend fun reverseGeocode(
        latitude: Double,
        longitude: Double,
    ): GeocodingResult = result
}

private val FOUND_RESULT =
    PlaceDetailsResult.Found(
        name = "Lviv Coffee Manufacture",
        rating = 4.6,
        category = "Coffee shop",
        photoRefs = listOf("ref-1", "ref-2"),
        latitude = 49.8397,
        longitude = 24.0297,
    )

class PlaceImportPipelineTest {
    private fun pipeline(
        redirectResult: ShareLinkRedirectResult =
            ShareLinkRedirectResult.Resolved("https://www.google.com/maps/place/Lviv+Coffee+Manufacture/@49.8397,24.0297,17z"),
        lookupResult: PlaceDetailsResult = FOUND_RESULT,
        photoBytesByRef: Map<String, ByteArray?> = mapOf("ref-1" to byteArrayOf(1), "ref-2" to byteArrayOf(2)),
        failUploadIndices: Set<Int> = emptySet(),
        geocodingResult: GeocodingResult =
            GeocodingResult.Found(
                placeName = "Lviv Coffee Manufacture",
                city = "Lviv",
                cityPlaceId = "locality-place-id",
                countryCode = "UA",
            ),
    ) = PlaceImportPipeline(
        redirectResolver = FakeShareLinkRedirectResolver(redirectResult),
        detailsLookupClient = FakePlaceDetailsLookupClient(lookupResult),
        photoClient = FakePlacePhotoClient(photoBytesByRef),
        photoUploadClient = FakePlacePhotoUploadClient(failUploadIndices),
        placeGeocodingClient = FakePlaceGeocodingClient(geocodingResult),
        generatePlaceId = { "place-1" },
        clock = FixedClock,
    )

    @Test
    fun `happy path imports a place with photos rating and category`() =
        runTest {
            val result = pipeline().import(shareUrl = "https://maps.app.goo.gl/abc", tripId = "trip-1", addedByUserId = "owner-1")

            val imported = assertIs<PlaceImportResult.Imported>(result)
            assertEquals("place-1", imported.place.id)
            assertEquals("trip-1", imported.place.tripId)
            assertEquals("Lviv Coffee Manufacture", imported.place.name)
            assertEquals(4.6, imported.place.rating)
            assertEquals("Coffee shop", imported.place.category)
            assertEquals("Lviv", imported.place.city)
            assertEquals("locality-place-id", imported.place.cityPlaceId)
            assertEquals("UA", imported.place.countryCode)
            assertEquals(
                listOf(
                    PlacePhoto(photoRef = "ref-1", remoteUrl = "https://storage/place-photos/place-1/0"),
                    PlacePhoto(photoRef = "ref-2", remoteUrl = "https://storage/place-photos/place-1/1"),
                ),
                imported.place.photos,
            )
            assertEquals(FIXED_NOW, imported.place.createdAt)
            assertEquals(FIXED_NOW, imported.place.updatedAt)
        }

    @Test
    fun `name-only link with no coordinates still imports using the details lookup's coordinates`() =
        runTest {
            val nameOnlyRedirect =
                ShareLinkRedirectResult.Resolved(
                    "https://www.google.com/maps/place/Global+Solar,+Some+Street,+Vinnytsia/data=!4m2!3m1!1s0xabc:0xdef",
                )
            val pipeline = pipeline(redirectResult = nameOnlyRedirect)

            val result = pipeline.import(shareUrl = "https://maps.app.goo.gl/abc", tripId = "trip-1", addedByUserId = "owner-1")

            val imported = assertIs<PlaceImportResult.Imported>(result)
            assertEquals(49.8397, imported.place.latitude)
            assertEquals(24.0297, imported.place.longitude)
        }

    @Test
    fun `lookup not found surfaces as NotFound`() =
        runTest {
            val result =
                pipeline(lookupResult = PlaceDetailsResult.NotFound)
                    .import(shareUrl = "https://maps.app.goo.gl/abc", tripId = "trip-1", addedByUserId = "owner-1")

            assertEquals(PlaceImportResult.NotFound, result)
        }

    @Test
    fun `lookup failure surfaces as Failure`() =
        runTest {
            val cause = IllegalStateException("boom")
            val result =
                pipeline(lookupResult = PlaceDetailsResult.Failure(cause))
                    .import(shareUrl = "https://maps.app.goo.gl/abc", tripId = "trip-1", addedByUserId = "owner-1")

            val failure = assertIs<PlaceImportResult.Failure>(result)
            assertEquals(cause, failure.cause)
        }

    @Test
    fun `redirect resolve failure surfaces as Failure without calling the lookup client`() =
        runTest {
            val cause = IllegalStateException("network down")
            val lookupClient = FakePlaceDetailsLookupClient(FOUND_RESULT)
            val pipeline =
                PlaceImportPipeline(
                    redirectResolver = FakeShareLinkRedirectResolver(ShareLinkRedirectResult.Failure(cause)),
                    detailsLookupClient = lookupClient,
                    photoClient = FakePlacePhotoClient(emptyMap()),
                    photoUploadClient = FakePlacePhotoUploadClient(),
                    placeGeocodingClient = FakePlaceGeocodingClient(),
                    generatePlaceId = { "place-1" },
                    clock = FixedClock,
                )

            val result = pipeline.import(shareUrl = "https://maps.app.goo.gl/abc", tripId = "trip-1", addedByUserId = "owner-1")

            val failure = assertIs<PlaceImportResult.Failure>(result)
            assertEquals(cause, failure.cause)
            assertEquals(null, lookupClient.lastQuery)
        }

    @Test
    fun `one photo failing to fetch still imports the place with that photo's remoteUrl left null`() =
        runTest {
            val result =
                pipeline(photoBytesByRef = mapOf("ref-1" to null, "ref-2" to byteArrayOf(2)))
                    .import(shareUrl = "https://maps.app.goo.gl/abc", tripId = "trip-1", addedByUserId = "owner-1")

            val imported = assertIs<PlaceImportResult.Imported>(result)
            assertEquals(
                listOf(
                    PlacePhoto(photoRef = "ref-1", remoteUrl = null),
                    PlacePhoto(photoRef = "ref-2", remoteUrl = "https://storage/place-photos/place-1/1"),
                ),
                imported.place.photos,
            )
        }

    @Test
    fun `one photo failing to upload still imports the place with that photo's remoteUrl left null`() =
        runTest {
            val result =
                pipeline(failUploadIndices = setOf(0))
                    .import(shareUrl = "https://maps.app.goo.gl/abc", tripId = "trip-1", addedByUserId = "owner-1")

            val imported = assertIs<PlaceImportResult.Imported>(result)
            assertEquals(
                listOf(
                    PlacePhoto(photoRef = "ref-1", remoteUrl = null),
                    PlacePhoto(photoRef = "ref-2", remoteUrl = "https://storage/place-photos/place-1/1"),
                ),
                imported.place.photos,
            )
        }

    @Test
    fun `all photos failing still imports the place with every photo kept and a null remoteUrl`() =
        runTest {
            val result =
                pipeline(photoBytesByRef = mapOf("ref-1" to null, "ref-2" to null))
                    .import(shareUrl = "https://maps.app.goo.gl/abc", tripId = "trip-1", addedByUserId = "owner-1")

            val imported = assertIs<PlaceImportResult.Imported>(result)
            assertEquals(
                listOf(PlacePhoto(photoRef = "ref-1", remoteUrl = null), PlacePhoto(photoRef = "ref-2", remoteUrl = null)),
                imported.place.photos,
            )
            assertEquals("Lviv Coffee Manufacture", imported.place.name)
        }

    @Test
    fun `geocoding not finding a city still imports the place with null city fields`() =
        runTest {
            val result =
                pipeline(geocodingResult = GeocodingResult.NotFound)
                    .import(shareUrl = "https://maps.app.goo.gl/abc", tripId = "trip-1", addedByUserId = "owner-1")

            val imported = assertIs<PlaceImportResult.Imported>(result)
            assertEquals(null, imported.place.city)
            assertEquals(null, imported.place.cityPlaceId)
            assertEquals(null, imported.place.countryCode)
            assertEquals("Lviv Coffee Manufacture", imported.place.name)
        }

    @Test
    fun `geocoding failure still imports the place with null city fields`() =
        runTest {
            val result =
                pipeline(geocodingResult = GeocodingResult.Failure(IllegalStateException("boom")))
                    .import(shareUrl = "https://maps.app.goo.gl/abc", tripId = "trip-1", addedByUserId = "owner-1")

            val imported = assertIs<PlaceImportResult.Imported>(result)
            assertEquals(null, imported.place.city)
            assertEquals(null, imported.place.cityPlaceId)
            assertEquals(null, imported.place.countryCode)
        }

    private fun placeWithPhotos(photos: List<PlacePhoto>) =
        PlaceCandidate(
            id = "place-1",
            tripId = "trip-1",
            name = "Lviv Coffee Manufacture",
            latitude = 49.8397,
            longitude = 24.0297,
            note = null,
            addedByUserId = "owner-1",
            syncStatus = SyncStatus.PENDING,
            createdAt = FIXED_NOW,
            updatedAt = FIXED_NOW,
            photos = photos,
        )

    @Test
    fun `retryIncomplete uploads a photo still missing its remoteUrl at its original index`() =
        runTest {
            val place =
                placeWithPhotos(
                    listOf(
                        PlacePhoto(photoRef = "ref-1", remoteUrl = "https://storage/place-photos/place-1/0"),
                        PlacePhoto(photoRef = "ref-2", remoteUrl = null),
                    ),
                )

            val retried = pipeline().retryIncomplete(place)

            assertEquals(
                listOf(
                    PlacePhoto(photoRef = "ref-1", remoteUrl = "https://storage/place-photos/place-1/0"),
                    PlacePhoto(photoRef = "ref-2", remoteUrl = "https://storage/place-photos/place-1/1"),
                ),
                retried.photos,
            )
        }

    @Test
    fun `retryIncomplete still failing keeps the photo unuploaded without throwing`() =
        runTest {
            val place = placeWithPhotos(listOf(PlacePhoto(photoRef = "ref-1", remoteUrl = null)))

            val retried =
                pipeline(photoBytesByRef = mapOf("ref-1" to null, "ref-2" to byteArrayOf(2))).retryIncomplete(place)

            assertEquals(listOf(PlacePhoto(photoRef = "ref-1", remoteUrl = null)), retried.photos)
        }

    @Test
    fun `retryIncomplete on a fully-uploaded place returns it unchanged`() =
        runTest {
            val place =
                placeWithPhotos(listOf(PlacePhoto(photoRef = "ref-1", remoteUrl = "https://storage/existing")))

            val retried = pipeline().retryIncomplete(place)

            assertEquals(place, retried)
        }
}
