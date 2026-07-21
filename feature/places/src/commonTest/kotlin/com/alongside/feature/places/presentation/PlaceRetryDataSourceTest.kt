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

class PlaceRetryDataSourceTest {
    private val placeCandidateRepository = RecordingPlaceCandidateRepository()
    private val pairingRepository = FakePairingRepository(initialActiveTrip = fakeTrip())

    private fun dataSource(photoBytesByRef: Map<String, ByteArray?> = mapOf("ref-2" to byteArrayOf(1))) =
        PlaceRetryDataSource(
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

    private fun placeCandidate(photos: List<PlacePhoto>) =
        PlaceCandidate(
            id = "place-1",
            tripId = "trip-1",
            name = "Lviv Coffee Manufacture",
            latitude = 49.8397,
            longitude = 24.0297,
            note = null,
            addedByUserId = "owner-1",
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

            dataSource().retryIncompletePlaces(listOf(incomplete))

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

            dataSource().retryIncompletePlaces(listOf(complete))

            assertTrue(placeCandidateRepository.upserted.isEmpty())
        }

    @Test
    fun `a still-failing retry does not throw and leaves the photo unuploaded`() =
        runTest {
            val incomplete = placeCandidate(photos = listOf(PlacePhoto(photoRef = "ref-1", remoteUrl = null)))

            dataSource(photoBytesByRef = mapOf("ref-1" to null)).retryIncompletePlaces(listOf(incomplete))

            assertTrue(placeCandidateRepository.upserted.isEmpty())
        }
}
