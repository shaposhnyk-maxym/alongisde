package com.alongside.feature.places.presentation

import com.alongside.feature.places.FakePairingRepository
import com.alongside.feature.places.FakePlaceContentPuller
import com.alongside.feature.places.fakeTrip
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlaceContentPullCoordinatorTest {
    @Test
    fun `pulls the active trip's content for the owning user`() =
        runTest {
            val pairingRepository = FakePairingRepository(initialActiveTrip = fakeTrip(id = "trip-1"))
            val placeContentPuller = FakePlaceContentPuller()
            val coordinator = PlaceContentPullCoordinator(pairingRepository, placeContentPuller)

            coordinator.pullActiveTripContent("uid-1")

            assertEquals(listOf("trip-1" to "uid-1"), placeContentPuller.pulls)
        }

    @Test
    fun `no active trip pulls nothing`() =
        runTest {
            val pairingRepository = FakePairingRepository(initialActiveTrip = null)
            val placeContentPuller = FakePlaceContentPuller()
            val coordinator = PlaceContentPullCoordinator(pairingRepository, placeContentPuller)

            coordinator.pullActiveTripContent("uid-1")

            assertTrue(placeContentPuller.pulls.isEmpty())
        }
}
