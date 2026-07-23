package com.alongside.core.domain.trip

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class DefaultTripManagementRepositoryTest {
    private val tripRepository = FakeTripRepository()
    private val repository = DefaultTripManagementRepository(tripRepository)

    @Test
    fun `owner deletes their trip`() =
        runTest {
            val trip = tripManagementTestTrip(ownerId = "owner-1", memberId = "member-1")
            tripRepository.seed(trip)

            val result = repository.deleteTrip(trip.id, callerId = "owner-1")

            assertEquals(DeleteTripResult.Deleted, result)
            assertEquals(listOf(trip.id), tripRepository.deletedIds)
            assertNull(tripRepository.getById(trip.id))
        }

    @Test
    fun `member cannot delete the trip`() =
        runTest {
            val trip = tripManagementTestTrip(ownerId = "owner-1", memberId = "member-1")
            tripRepository.seed(trip)

            val result = repository.deleteTrip(trip.id, callerId = "member-1")

            assertEquals(DeleteTripResult.NotOwner, result)
            assertEquals(emptyList(), tripRepository.deletedIds)
            assertEquals(trip, tripRepository.getById(trip.id))
        }

    @Test
    fun `deleting an unknown trip id returns NotFound`() =
        runTest {
            val result = repository.deleteTrip("no-such-trip", callerId = "owner-1")

            assertEquals(DeleteTripResult.NotFound, result)
        }

    @Test
    fun `member leaving clears their slot and the trip survives`() =
        runTest {
            val trip = tripManagementTestTrip(ownerId = "owner-1", memberId = "member-1")
            tripRepository.seed(trip)

            val result = repository.leaveTrip(trip.id, callerId = "member-1")

            val left = assertIs<LeaveTripResult.Left>(result)
            assertNull(left.trip.memberId)
            assertEquals("owner-1", left.trip.ownerId)
            assertEquals(left.trip, tripRepository.getById(trip.id))
        }

    @Test
    fun `owner leaving with a member present transfers ownership`() =
        runTest {
            val trip = tripManagementTestTrip(ownerId = "owner-1", memberId = "member-1")
            tripRepository.seed(trip)

            val result = repository.leaveTrip(trip.id, callerId = "owner-1")

            val transferred = assertIs<LeaveTripResult.OwnershipTransferred>(result)
            assertEquals("member-1", transferred.trip.ownerId)
            assertNull(transferred.trip.memberId)
            assertEquals(transferred.trip, tripRepository.getById(trip.id))
        }

    @Test
    fun `owner leaving a solo trip deletes it`() =
        runTest {
            val trip = tripManagementTestTrip(ownerId = "owner-1", memberId = null)
            tripRepository.seed(trip)

            val result = repository.leaveTrip(trip.id, callerId = "owner-1")

            assertEquals(LeaveTripResult.Deleted, result)
            assertEquals(listOf(trip.id), tripRepository.deletedIds)
            assertNull(tripRepository.getById(trip.id))
        }

    @Test
    fun `a stranger leaving returns NotFound`() =
        runTest {
            val trip = tripManagementTestTrip(ownerId = "owner-1", memberId = "member-1")
            tripRepository.seed(trip)

            val result = repository.leaveTrip(trip.id, callerId = "stranger")

            assertEquals(LeaveTripResult.NotFound, result)
            assertEquals(emptyList(), tripRepository.deletedIds)
            assertEquals(emptyList(), tripRepository.upserted)
        }

    @Test
    fun `leaving an unknown trip id returns NotFound`() =
        runTest {
            val result = repository.leaveTrip("no-such-trip", callerId = "owner-1")

            assertEquals(LeaveTripResult.NotFound, result)
        }
}
