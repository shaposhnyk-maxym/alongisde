package com.alongside.data.trip

import com.alongside.core.domain.trip.DefaultTripManagementRepository
import com.alongside.core.domain.trip.DeleteTripResult
import com.alongside.core.domain.trip.LeaveTripResult
import com.alongside.core.network.firestore.model.FirestoreDocument
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
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

private val FIXED_NOW = Instant.fromEpochMilliseconds(1_752_800_000_000)

private object ConfirmedRepoFixedClock : Clock {
    override fun now(): Instant = FIXED_NOW
}

class ConfirmedTripManagementRepositoryTest {
    private val local = RecordingTripRepository()
    private val store = InMemorySyncOperationStore()
    private val networkClient = RecordingSyncNetworkClient()
    private val remoteReader = FakeRemoteDocumentReader()
    private val syncingTrips =
        SyncingTripRepository(
            local = local,
            store = store,
            backgroundWorkScheduler = FakeBackgroundWorkScheduler(),
            clock = ConfirmedRepoFixedClock,
        )
    private val syncCoordinator =
        SyncCoordinator(
            store = store,
            processor = SyncQueueProcessor(networkClient, MaxAttemptsRetryPolicy(2)),
            remoteReader = remoteReader,
            bindings = listOf(TripSyncEntityBinding(local)),
        )
    private val repository =
        ConfirmedTripManagementRepository(
            delegate = DefaultTripManagementRepository(syncingTrips),
            syncCoordinator = syncCoordinator,
            tripRepository = syncingTrips,
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
    fun `leaveTrip reports SyncFailed, not success, when a concurrent remote write silently discards it`() =
        runTest {
            // The preflight conflict check for an UPSERT applies the remote copy locally and
            // drops the operation from the queue when remote looks newer (SyncCoordinator.
            // preflight's REMOTE_WON branch) - that op never reaches processor.processAll, so it
            // shows up in neither succeeded nor failed. A confirmation check that only looks at
            // those two lists would report the leave as confirmed even though it was just
            // silently reverted back to memberId="member-1" by applyRemote.
            val trip = testTrip(id = "trip-1", ownerId = "owner-1", memberId = "member-1", updatedAt = FIXED_NOW)
            local.upsert(trip)
            remoteReader.documents["trip-1"] =
                FirestoreDocument(fields = TripFirestoreMapper.toFields(trip.copy(updatedAt = FIXED_NOW + 1.minutes)))

            val result = repository.leaveTrip("trip-1", "member-1")

            assertEquals(LeaveTripResult.SyncFailed, result)
            assertEquals("member-1", local.getById("trip-1")?.memberId)
        }

    @Test
    fun `leaveTrip NotFound passes through without attempting a sync`() =
        runTest {
            val result = repository.leaveTrip("no-such-trip", "owner-1")

            assertEquals(LeaveTripResult.NotFound, result)
            assertEquals(emptyList(), networkClient.pushed)
        }
}
