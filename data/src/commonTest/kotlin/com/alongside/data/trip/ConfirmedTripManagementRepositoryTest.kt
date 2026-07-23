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
    fun `leaveTrip is never silently discarded by a concurrent remote write claiming a newer timestamp`() =
        runTest {
            // Leaving goes through TripRepository.forceUpsert, so the underlying SyncOperation is
            // FORCE_UPSERT - SyncCoordinator.preflight skips the conflict-check read entirely for
            // it (same as DELETE), so a remote document claiming a "newer" updatedAt (clock skew
            // between the two devices, or any other concurrent write) can never silently revert
            // the leave back to memberId="member-1" the way a plain UPSERT's preflight could.
            val trip = testTrip(id = "trip-1", ownerId = "owner-1", memberId = "member-1", updatedAt = FIXED_NOW)
            local.upsert(trip)
            remoteReader.documents["trip-1"] =
                FirestoreDocument(fields = TripFirestoreMapper.toFields(trip.copy(updatedAt = FIXED_NOW + 1.minutes)))

            val result = repository.leaveTrip("trip-1", "member-1")

            assertIs<LeaveTripResult.Left>(result)
            assertEquals(emptyList(), remoteReader.readDocumentIds)
            assertEquals(null, local.getById("trip-1")?.memberId)
        }

    @Test
    fun `leaveTrip NotFound passes through without attempting a sync`() =
        runTest {
            val result = repository.leaveTrip("no-such-trip", "owner-1")

            assertEquals(LeaveTripResult.NotFound, result)
            assertEquals(emptyList(), networkClient.pushed)
        }
}
