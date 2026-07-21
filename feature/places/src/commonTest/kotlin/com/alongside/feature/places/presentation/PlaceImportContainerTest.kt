package com.alongside.feature.places.presentation

import com.alongside.core.domain.place.importing.PlaceDetailsResult
import com.alongside.core.domain.place.importing.PlaceImportPipeline
import com.alongside.core.domain.place.importing.ShareLinkRedirectResult
import com.alongside.core.model.SyncStatus
import com.alongside.core.model.place.PlaceCandidate
import com.alongside.feature.places.FakeAuthSessionCache
import com.alongside.feature.places.FakePairingRepository
import com.alongside.feature.places.FakePlaceDetailsLookupClient
import com.alongside.feature.places.FakePlaceGeocodingClient
import com.alongside.feature.places.FakePlacePhotoClient
import com.alongside.feature.places.FakePlacePhotoUploadClient
import com.alongside.feature.places.FakeShareLinkRedirectResolver
import com.alongside.feature.places.RecordingPlaceCandidateRepository
import com.alongside.feature.places.fakeTrip
import com.alongside.feature.places.testAuthSession
import kotlinx.coroutines.test.runTest
import org.orbitmvi.orbit.test.test
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

private val FIXED_NOW = Instant.fromEpochMilliseconds(1_752_800_000_000)

private object FixedClock : Clock {
    override fun now(): Instant = FIXED_NOW
}

private const val SHARE_URL = "https://maps.app.goo.gl/Mnzn3A1DSQYoXsDw5"
private const val SHARE_TEXT = "Lviv Coffee Manufacture\n$SHARE_URL"

private val FOUND_RESULT =
    PlaceDetailsResult.Found(
        name = "Lviv Coffee Manufacture",
        rating = 4.6,
        category = "Coffee shop",
        photoRefs = emptyList(),
        latitude = 49.8397,
        longitude = 24.0297,
    )

private val EXPECTED_PLACE =
    PlaceCandidate(
        id = "place-1",
        tripId = "trip-1",
        name = "Lviv Coffee Manufacture",
        latitude = 49.8397,
        longitude = 24.0297,
        note = null,
        addedByUserId = "uid-1",
        ownerSwipe = null,
        memberSwipe = null,
        syncStatus = SyncStatus.PENDING,
        createdAt = FIXED_NOW,
        updatedAt = FIXED_NOW,
        photos = emptyList(),
        rating = 4.6,
        category = "Coffee shop",
        city = "Lviv",
    )

class PlaceImportContainerTest {
    private val placeCandidateRepository = RecordingPlaceCandidateRepository()
    private val pairingRepository =
        FakePairingRepository(initialActiveTrip = fakeTrip(id = "trip-1", ownerId = "uid-1"))
    private val authSessionCache = FakeAuthSessionCache(testAuthSession("uid-1"))

    private fun pipeline(
        redirectResult: ShareLinkRedirectResult =
            ShareLinkRedirectResult.Resolved(
                "https://www.google.com/maps/place/Lviv+Coffee+Manufacture/@49.8397,24.0297,17z",
            ),
        lookupResult: PlaceDetailsResult = FOUND_RESULT,
    ) = PlaceImportPipeline(
        redirectResolver = FakeShareLinkRedirectResolver(redirectResult),
        detailsLookupClient = FakePlaceDetailsLookupClient(lookupResult),
        photoClient = FakePlacePhotoClient(),
        photoUploadClient = FakePlacePhotoUploadClient(),
        placeGeocodingClient = FakePlaceGeocodingClient(),
        generatePlaceId = { "place-1" },
        clock = FixedClock,
    )

    private fun containerUnderTest(
        shareText: String = SHARE_TEXT,
        pipeline: PlaceImportPipeline = pipeline(),
    ) = PlaceImportContainer(
        shareText = shareText,
        pipeline = pipeline,
        placeCandidateRepository = placeCandidateRepository,
        authSessionCache = authSessionCache,
        pairingRepository = pairingRepository,
    )

    @Test
    fun `a valid share link resolves to the FOUND state with the imported place`() =
        runTest {
            containerUnderTest().test(this) {
                runOnCreate()
                expectState { copy(status = PlaceImportStatus.FOUND, place = EXPECTED_PLACE) }
            }
        }

    @Test
    fun `lookup not found resolves to the NOT_FOUND state`() =
        runTest {
            containerUnderTest(pipeline = pipeline(lookupResult = PlaceDetailsResult.NotFound)).test(this) {
                runOnCreate()
                expectState { copy(status = PlaceImportStatus.NOT_FOUND) }
            }
        }

    @Test
    fun `a pipeline failure resolves to the ERROR state with the failure's message`() =
        runTest {
            val cause = IllegalStateException("boom")
            val pipeline =
                PlaceImportPipeline(
                    redirectResolver = FakeShareLinkRedirectResolver(ShareLinkRedirectResult.Failure(cause)),
                    detailsLookupClient = FakePlaceDetailsLookupClient(FOUND_RESULT),
                    photoClient = FakePlacePhotoClient(),
                    photoUploadClient = FakePlacePhotoUploadClient(),
                    placeGeocodingClient = FakePlaceGeocodingClient(),
                    generatePlaceId = { "place-1" },
                    clock = FixedClock,
                )

            containerUnderTest(pipeline = pipeline).test(this) {
                runOnCreate()
                expectState { copy(status = PlaceImportStatus.ERROR, errorMessage = "boom") }
            }
        }

    @Test
    fun `share text with no url resolves to ERROR without calling the pipeline`() =
        runTest {
            containerUnderTest(shareText = "just some text, no link").test(this) {
                runOnCreate()
                expectState {
                    copy(
                        status = PlaceImportStatus.ERROR,
                        errorMessage = "No Google Maps link found in the shared text",
                    )
                }
            }
            assertTrue(placeCandidateRepository.upserted.isEmpty())
        }

    @Test
    fun `no active trip resolves to ERROR without calling the pipeline`() =
        runTest {
            pairingRepository.activeTrip.value = null

            containerUnderTest().test(this) {
                runOnCreate()
                expectState {
                    copy(status = PlaceImportStatus.ERROR, errorMessage = "No active trip to import this place into")
                }
            }
        }

    @Test
    fun `Accept persists the imported place and fires Imported`() =
        runTest {
            containerUnderTest().test(this) {
                runOnCreate()
                expectState { copy(status = PlaceImportStatus.FOUND, place = EXPECTED_PLACE) }

                containerHost.onIntent(PlaceImportIntent.Accept)
                expectSideEffect(PlaceImportSideEffect.Imported)
            }
            assertEquals(1, placeCandidateRepository.upserted.size)
            assertEquals("place-1", placeCandidateRepository.upserted.single().id)
        }

    @Test
    fun `Discard does not persist and fires Discarded`() =
        runTest {
            containerUnderTest().test(this) {
                runOnCreate()
                expectState { copy(status = PlaceImportStatus.FOUND, place = EXPECTED_PLACE) }

                containerHost.onIntent(PlaceImportIntent.Discard)
                expectSideEffect(PlaceImportSideEffect.Discarded)
            }
            assertTrue(placeCandidateRepository.upserted.isEmpty())
        }
}
