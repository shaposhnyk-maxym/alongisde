package com.alongside.data.trip

import com.alongside.core.domain.trip.DefaultTripManagementRepository
import com.alongside.core.domain.trip.DeleteTripResult
import com.alongside.core.domain.trip.LeaveTripResult
import com.alongside.core.network.queue.MaxAttemptsRetryPolicy
import com.alongside.core.network.queue.SyncQueueProcessor
import com.alongside.data.FakeBackgroundWorkScheduler
import com.alongside.data.sync.FakeRemoteDocumentReader
import com.alongside.data.sync.InMemorySyncOperationStore
import com.alongside.data.sync.RecordingSyncNetworkClient
import com.alongside.data.sync.SyncCoordinator
import com.alongside.data.testTrip
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ConfirmedTripManagementRepositoryTest {
    private val local = RecordingTripRepository()
    private val store = InMemorySyncOperationStore()
    private val networkClient = RecordingSyncNetworkClient()
    private val syncingTrips =
        SyncingTripRepository(
            local = local,
            store = store,
            backgroundWorkScheduler = FakeBackgroundWorkScheduler(),
        )
    private val syncCoordinator =
        SyncCoordinator(
            store = store,
            processor = SyncQueueProcessor(networkClient, MaxAttemptsRetryPolicy(2)),
            remoteReader = FakeRemoteDocumentReader(),
            bindings = listOf(TripSyncEntityBinding(local)),
        )
    private val repository =
        ConfirmedTripManagementRepository(
            delegate = DefaultTripManagementRepository(syncingTrips),
            syncCoordinator = syncCoordinator,
        )

    @Test
    fun `deleteTrip returns Deleted only once the remote push is confirmed`() =
        runTest {
            local.upsert(testTrip(id = "trip-1", ownerId = "owner-1"))

            val result = repository.deleteTrip("trip-1", "owner-1")

            assertEquals(DeleteTripResult.Deleted, result)
            assertEquals(listOf("trip-1"), networkClient.pushed.map { it.documentId })
        }

    @Test
    fun `deleteTrip returns SyncFailed when the push fails, leaving it queued for retry`() =
        runTest {
            local.upsert(testTrip(id = "trip-1", ownerId = "owner-1"))
            networkClient.failAll = true

            val result = repository.deleteTrip("trip-1", "owner-1")

            assertEquals(DeleteTripResult.SyncFailed, result)
            assertEquals(1, store.loadAll().size)
        }

    @Test
    fun `deleteTrip authorization failures pass through without attempting a sync`() =
        runTest {
            local.upsert(testTrip(id = "trip-1", ownerId = "owner-1", memberId = "member-1"))

            val result = repository.deleteTrip("trip-1", "member-1")

            assertEquals(DeleteTripResult.NotOwner, result)
            assertEquals(emptyList(), networkClient.pushed)
        }

    @Test
    fun `leaveTrip returns the delegate's result once the remote push is confirmed`() =
        runTest {
            local.upsert(testTrip(id = "trip-1", ownerId = "owner-1", memberId = "member-1"))

            val result = repository.leaveTrip("trip-1", "member-1")

            val left = assertIs<LeaveTripResult.Left>(result)
            assertEquals(null, left.trip.memberId)
            assertEquals(listOf("trip-1"), networkClient.pushed.map { it.documentId })
        }

    @Test
    fun `leaveTrip returns SyncFailed when the push fails`() =
        runTest {
            local.upsert(testTrip(id = "trip-1", ownerId = "owner-1", memberId = "member-1"))
            networkClient.failAll = true

            val result = repository.leaveTrip("trip-1", "member-1")

            assertEquals(LeaveTripResult.SyncFailed, result)
        }

    @Test
    fun `leaveTrip NotFound passes through without attempting a sync`() =
        runTest {
            val result = repository.leaveTrip("no-such-trip", "owner-1")

            assertEquals(LeaveTripResult.NotFound, result)
            assertEquals(emptyList(), networkClient.pushed)
        }
}
