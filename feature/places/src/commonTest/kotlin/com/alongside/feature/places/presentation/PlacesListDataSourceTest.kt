package com.alongside.feature.places.presentation

import com.alongside.core.model.SyncStatus
import com.alongside.core.model.place.PlaceCandidate
import com.alongside.feature.places.FakePairingRepository
import com.alongside.feature.places.FakePlaceContentPuller
import com.alongside.feature.places.RecordingPlaceCandidateRepository
import com.alongside.feature.places.fakeTrip
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

private fun place(
    id: String,
    tripId: String,
) = PlaceCandidate(
    id = id,
    tripId = tripId,
    name = "Place $id",
    latitude = 0.0,
    longitude = 0.0,
    note = null,
    addedByUserId = "owner-1",
    syncStatus = SyncStatus.PENDING,
    createdAt = Instant.fromEpochMilliseconds(0),
    updatedAt = Instant.fromEpochMilliseconds(0),
)

class PlacesListDataSourceTest {
    private val placeCandidateRepository = RecordingPlaceCandidateRepository()

    @Test
    fun `no active trip observes an empty list`() =
        runTest {
            val pairingRepository = FakePairingRepository(initialActiveTrip = null)
            val dataSource = PlacesListDataSource(pairingRepository, placeCandidateRepository, FakePlaceContentPuller())
            val updates = mutableListOf<List<PlaceCandidate>>()

            backgroundScope.launch { dataSource.observe("uid-1") { updates += it } }
            runCurrent()

            assertEquals(listOf(emptyList()), updates)
        }

    @Test
    fun `an active trip forwards observeByTrip's places`() =
        runTest {
            val pairingRepository = FakePairingRepository(initialActiveTrip = fakeTrip(id = "trip-1"))
            val dataSource = PlacesListDataSource(pairingRepository, placeCandidateRepository, FakePlaceContentPuller())
            placeCandidateRepository.upsert(place(id = "place-1", tripId = "trip-1"))
            val updates = mutableListOf<List<PlaceCandidate>>()

            backgroundScope.launch { dataSource.observe("uid-1") { updates += it } }
            runCurrent()

            assertEquals(listOf("place-1"), updates.last().map { it.id })
        }

    @Test
    fun `trip changing switches to the new trip's places`() =
        runTest {
            val pairingRepository = FakePairingRepository(initialActiveTrip = fakeTrip(id = "trip-1"))
            val dataSource = PlacesListDataSource(pairingRepository, placeCandidateRepository, FakePlaceContentPuller())
            placeCandidateRepository.upsert(place(id = "place-1", tripId = "trip-1"))
            placeCandidateRepository.upsert(place(id = "place-2", tripId = "trip-2"))
            val updates = mutableListOf<List<PlaceCandidate>>()
            backgroundScope.launch { dataSource.observe("uid-1") { updates += it } }
            runCurrent()

            pairingRepository.activeTrip.value = fakeTrip(id = "trip-2")
            runCurrent()

            assertEquals(listOf("place-2"), updates.last().map { it.id })
        }
}
