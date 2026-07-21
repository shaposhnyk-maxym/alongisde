package com.alongside.core.domain.place.importing

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
    ) = PlaceImportPipeline(
        redirectResolver = FakeShareLinkRedirectResolver(redirectResult),
        detailsLookupClient = FakePlaceDetailsLookupClient(lookupResult),
        photoClient = FakePlacePhotoClient(photoBytesByRef),
        photoUploadClient = FakePlacePhotoUploadClient(failUploadIndices),
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
            assertEquals(
                listOf("https://storage/place-photos/place-1/0", "https://storage/place-photos/place-1/1"),
                imported.place.photoUrls,
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
                    generatePlaceId = { "place-1" },
                    clock = FixedClock,
                )

            val result = pipeline.import(shareUrl = "https://maps.app.goo.gl/abc", tripId = "trip-1", addedByUserId = "owner-1")

            val failure = assertIs<PlaceImportResult.Failure>(result)
            assertEquals(cause, failure.cause)
            assertEquals(null, lookupClient.lastQuery)
        }

    @Test
    fun `one photo failing to fetch still imports the place with the other photo's url`() =
        runTest {
            val result =
                pipeline(photoBytesByRef = mapOf("ref-1" to null, "ref-2" to byteArrayOf(2)))
                    .import(shareUrl = "https://maps.app.goo.gl/abc", tripId = "trip-1", addedByUserId = "owner-1")

            val imported = assertIs<PlaceImportResult.Imported>(result)
            assertEquals(listOf("https://storage/place-photos/place-1/1"), imported.place.photoUrls)
        }

    @Test
    fun `one photo failing to upload still imports the place with the other photo's url`() =
        runTest {
            val result =
                pipeline(failUploadIndices = setOf(0))
                    .import(shareUrl = "https://maps.app.goo.gl/abc", tripId = "trip-1", addedByUserId = "owner-1")

            val imported = assertIs<PlaceImportResult.Imported>(result)
            assertEquals(listOf("https://storage/place-photos/place-1/1"), imported.place.photoUrls)
        }

    @Test
    fun `all photos failing still imports the place with an empty photoUrls list`() =
        runTest {
            val result =
                pipeline(photoBytesByRef = mapOf("ref-1" to null, "ref-2" to null))
                    .import(shareUrl = "https://maps.app.goo.gl/abc", tripId = "trip-1", addedByUserId = "owner-1")

            val imported = assertIs<PlaceImportResult.Imported>(result)
            assertEquals(emptyList(), imported.place.photoUrls)
            assertEquals("Lviv Coffee Manufacture", imported.place.name)
        }
}
