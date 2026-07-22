package com.alongside.feature.places.presentation

import com.alongside.core.model.SyncStatus
import com.alongside.core.model.place.PlaceCandidate
import com.alongside.feature.places.FakeAuthSessionCache
import com.alongside.feature.places.FakePairingRepository
import com.alongside.feature.places.RecordingPlaceCandidateRepository
import com.alongside.feature.places.fakeTrip
import com.alongside.feature.places.testAuthSession
import kotlinx.coroutines.test.runTest
import org.orbitmvi.orbit.test.test
import kotlin.test.Test
import kotlin.time.Instant

private val FIXED_NOW = Instant.fromEpochMilliseconds(1_752_800_000_000)

private fun place(id: String) =
    PlaceCandidate(
        id = id,
        tripId = "trip-1",
        name = "Place $id",
        latitude = 0.0,
        longitude = 0.0,
        note = null,
        addedByUserId = "owner-1",
        syncStatus = SyncStatus.PENDING,
        createdAt = FIXED_NOW,
        updatedAt = FIXED_NOW,
    )

class PlacesListContainerTest {
    private val placeCandidateRepository = RecordingPlaceCandidateRepository()
    private val pairingRepository = FakePairingRepository(initialActiveTrip = fakeTrip(id = "trip-1"))
    private val authSessionCache = FakeAuthSessionCache(testAuthSession("uid-1"))
    private val placesListDataSource = PlacesListDataSource(pairingRepository, placeCandidateRepository)

    private fun containerUnderTest() = PlacesListContainer(authSessionCache, placesListDataSource)

    @Test
    fun `loads the active trip's places from Room`() =
        runTest {
            placeCandidateRepository.upsert(place("place-1"))

            containerUnderTest().test(this) {
                runOnCreate()
                expectState { copy(places = listOf(place("place-1")), isLoading = false) }
                cancelAndIgnoreRemainingItems()
            }
        }

    @Test
    fun `no places yet still flips isLoading to false with an empty list`() =
        runTest {
            containerUnderTest().test(this) {
                runOnCreate()
                expectState { copy(places = emptyList(), isLoading = false) }
                cancelAndIgnoreRemainingItems()
            }
        }
}
