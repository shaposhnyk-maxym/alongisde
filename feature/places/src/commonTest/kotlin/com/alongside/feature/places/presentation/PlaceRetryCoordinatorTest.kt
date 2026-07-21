package com.alongside.feature.places.presentation

import com.alongside.core.domain.place.importing.PlaceDetailsResult
import com.alongside.core.domain.place.importing.PlaceImportPipeline
import com.alongside.core.domain.place.importing.ShareLinkRedirectResult
import com.alongside.core.model.SyncStatus
import com.alongside.core.model.place.PlaceCandidate
import com.alongside.core.model.place.PlacePhoto
import com.alongside.feature.places.FakePairingRepository
import com.alongside.feature.places.FakePlaceDetailsLookupClient
import com.alongside.feature.places.FakePlaceGeocodingClient
import com.alongside.feature.places.FakePlacePhotoClient
import com.alongside.feature.places.FakePlacePhotoUploadClient
import com.alongside.feature.places.FakeShareLinkRedirectResolver
import com.alongside.feature.places.RecordingPlaceCandidateRepository
import com.alongside.feature.places.fakeTrip
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

private val FIXED_NOW = Instant.fromEpochMilliseconds(1_752_800_000_000)

private object RetryFixedClock : Clock {
    override fun now(): Instant = FIXED_NOW
}

class PlaceRetryCoordinatorTest {
    private val placeCandidateRepository = RecordingPlaceCandidateRepository()
    private val pairingRepository = FakePairingRepository(initialActiveTrip = fakeTrip())

    private fun coordinator(photoBytesByRef: Map<String, ByteArray?> = mapOf("ref-2" to byteArrayOf(1))) =
        PlaceRetryCoordinator(
            pairingRepository = pairingRepository,
            placeCandidateRepository = placeCandidateRepository,
            pipeline =
                PlaceImportPipeline(
                    redirectResolver = FakeShareLinkRedirectResolver(ShareLinkRedirectResult.Failure(Exception())),
                    detailsLookupClient = FakePlaceDetailsLookupClient(PlaceDetailsResult.NotFound),
                    photoClient = FakePlacePhotoClient(photoBytesByRef),
                    photoUploadClient = FakePlacePhotoUploadClient(),
                    placeGeocodingClient = FakePlaceGeocodingClient(),
                    clock = RetryFixedClock,
                ),
        )

    private fun placeCandidate(
        photos: List<PlacePhoto>,
        id: String = "place-1",
        tripId: String = "trip-1",
        addedByUserId: String = "owner-1",
    ) = PlaceCandidate(
        id = id,
        tripId = tripId,
        name = "Lviv Coffee Manufacture",
        latitude = 49.8397,
        longitude = 24.0297,
        note = null,
        addedByUserId = addedByUserId,
        ownerSwipe = null,
        memberSwipe = null,
        syncStatus = SyncStatus.SYNCED,
        createdAt = FIXED_NOW,
        updatedAt = FIXED_NOW,
        photos = photos,
    )

    @Test
    fun `retries a place with a photo missing its remoteUrl and persists the healed place`() =
        runTest {
            val incomplete =
                placeCandidate(
                    photos =
                        listOf(
                            PlacePhoto(photoRef = "ref-1", remoteUrl = "https://storage/existing"),
                            PlacePhoto(photoRef = "ref-2", remoteUrl = null),
                        ),
                )

            coordinator().retryIncompletePlaces(listOf(incomplete))

            val healed = placeCandidateRepository.upserted.single()
            assertEquals(
                listOf(
                    PlacePhoto(photoRef = "ref-1", remoteUrl = "https://storage/existing"),
                    PlacePhoto(photoRef = "ref-2", remoteUrl = "https://storage/place-photos/place-1/1"),
                ),
                healed.photos,
            )
        }

    @Test
    fun `a fully-uploaded place is left untouched`() =
        runTest {
            val complete =
                placeCandidate(photos = listOf(PlacePhoto(photoRef = "ref-1", remoteUrl = "https://storage/x")))

            coordinator().retryIncompletePlaces(listOf(complete))

            assertTrue(placeCandidateRepository.upserted.isEmpty())
        }

    @Test
    fun `a still-failing retry does not throw and leaves the photo unuploaded`() =
        runTest {
            val incomplete = placeCandidate(photos = listOf(PlacePhoto(photoRef = "ref-1", remoteUrl = null)))

            coordinator(photoBytesByRef = mapOf("ref-1" to null)).retryIncompletePlaces(listOf(incomplete))

            assertTrue(placeCandidateRepository.upserted.isEmpty())
        }

    @Test
    fun `retryAllIncompletePlaces retries only the active trip's incomplete places`() =
        runTest {
            pairingRepository.activeTrip.value = fakeTrip(id = "trip-1")
            val incomplete =
                placeCandidate(id = "place-1", tripId = "trip-1", photos = listOf(PlacePhoto(photoRef = "ref-2", remoteUrl = null)))
            val complete =
                placeCandidate(
                    id = "place-2",
                    tripId = "trip-1",
                    photos = listOf(PlacePhoto(photoRef = "ref-x", remoteUrl = "https://storage/x")),
                )
            placeCandidateRepository.upsert(incomplete)
            placeCandidateRepository.upsert(complete)
            placeCandidateRepository.upserted.clear()

            coordinator().retryAllIncompletePlaces("owner-1")

            assertEquals(listOf("place-1"), placeCandidateRepository.upserted.map { it.id })
        }

    @Test
    fun `retryAllIncompletePlaces is a no-op when there is no active trip`() =
        runTest {
            pairingRepository.activeTrip.value = null

            coordinator().retryAllIncompletePlaces("owner-1")

            assertTrue(placeCandidateRepository.upserted.isEmpty())
        }
}
