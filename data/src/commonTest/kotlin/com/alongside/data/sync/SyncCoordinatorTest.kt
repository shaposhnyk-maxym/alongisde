package com.alongside.data.sync

import com.alongside.core.database.sync.PersistedSyncOperationStatus
import com.alongside.core.model.SyncStatus
import com.alongside.core.network.firestore.model.FirestoreDocument
import com.alongside.core.network.queue.MaxAttemptsRetryPolicy
import com.alongside.core.network.queue.SyncQueueProcessor
import com.alongside.data.testTrip
import com.alongside.data.trip.RecordingTripRepository
import com.alongside.data.trip.SyncingTripRepository
import com.alongside.data.trip.TripFirestoreMapper
import com.alongside.data.trip.TripSyncEntityBinding
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

private val FIXED_NOW = Instant.fromEpochMilliseconds(1_752_800_000_000)
private const val MAX_ATTEMPTS = 3

private object FixedClock : Clock {
    override fun now(): Instant = FIXED_NOW
}

class SyncCoordinatorTest {
    private val local = RecordingTripRepository()
    private val store = InMemorySyncOperationStore()
    private val networkClient = RecordingSyncNetworkClient()
    private val remoteReader = FakeRemoteDocumentReader()
    private var nextOpId = 0
    private val repository =
        SyncingTripRepository(
            local = local,
            store = store,
            clock = FixedClock,
            generateOpId = { "op-${++nextOpId}" },
        )
    private val coordinator =
        SyncCoordinator(
            store = store,
            processor = SyncQueueProcessor(networkClient, MaxAttemptsRetryPolicy(MAX_ATTEMPTS)),
            remoteReader = remoteReader,
            bindings = listOf(TripSyncEntityBinding(local)),
        )

    // --- Accept criterion 3: partial queue failure ---

    @Test
    fun `partial failure - the rest are processed and the failed one stays queued marked retry`() =
        runTest {
            repository.upsert(testTrip(id = "trip-1"))
            repository.upsert(testTrip(id = "trip-2", inviteCode = "CDEF23"))
            repository.upsert(testTrip(id = "trip-3", inviteCode = "EFGH23"))
            networkClient.failingDocumentIds = setOf("trip-2")

            val result = coordinator.sync()

            assertEquals(listOf("trip-1", "trip-3"), result.succeeded.map { it.documentId })
            assertEquals(listOf("trip-2"), result.failed.map { it.documentId })

            val remaining = store.loadAll().single()
            assertEquals("op-2", remaining.id)
            assertEquals(PersistedSyncOperationStatus.RETRY, remaining.status)
            assertEquals(MAX_ATTEMPTS, remaining.attempts)

            assertEquals(SyncStatus.SYNCED, local.getById("trip-1")?.syncStatus)
            assertEquals(SyncStatus.FAILED, local.getById("trip-2")?.syncStatus)
            assertEquals(SyncStatus.SYNCED, local.getById("trip-3")?.syncStatus)
        }

    @Test
    fun `a retry marked operation is pushed again on the next sync and recovers`() =
        runTest {
            repository.upsert(testTrip(id = "trip-2"))
            networkClient.failingDocumentIds = setOf("trip-2")
            coordinator.sync()

            networkClient.failingDocumentIds = emptySet()
            val result = coordinator.sync()

            assertEquals(listOf("trip-2"), result.succeeded.map { it.documentId })
            assertTrue(store.loadAll().isEmpty())
            assertEquals(SyncStatus.SYNCED, local.getById("trip-2")?.syncStatus)
        }

    // --- Accept criterion 2: deterministic last-write-wins ---

    @Test
    fun `remote newer - remote wins, nothing pushed, remote copy lands locally as SYNCED`() =
        runTest {
            repository.upsert(testTrip(id = "trip-1", memberId = null))
            val remoteTrip =
                testTrip(
                    id = "trip-1",
                    memberId = "member-remote",
                    updatedAt = FIXED_NOW + 1.minutes,
                )
            remoteReader.documents["trip-1"] = FirestoreDocument(fields = TripFirestoreMapper.toFields(remoteTrip))

            val result = coordinator.sync()

            assertEquals(emptyList(), networkClient.pushed)
            assertEquals(emptyList(), result.succeeded + result.failed)
            assertTrue(store.loadAll().isEmpty())
            val applied = local.getById("trip-1")
            assertEquals("member-remote", applied?.memberId)
            assertEquals(SyncStatus.SYNCED, applied?.syncStatus)
        }

    @Test
    fun `local newer - local wins and is pushed, remote copy is not applied`() =
        runTest {
            repository.upsert(testTrip(id = "trip-1", memberId = null))
            val remoteTrip =
                testTrip(
                    id = "trip-1",
                    memberId = "member-remote",
                    updatedAt = FIXED_NOW - 1.minutes,
                )
            remoteReader.documents["trip-1"] = FirestoreDocument(fields = TripFirestoreMapper.toFields(remoteTrip))

            val result = coordinator.sync()

            assertEquals(listOf("trip-1"), result.succeeded.map { it.documentId })
            assertEquals(null, local.getById("trip-1")?.memberId)
            assertEquals(SyncStatus.SYNCED, local.getById("trip-1")?.syncStatus)
        }

    @Test
    fun `equal timestamps - local wins the tie and is pushed`() =
        runTest {
            repository.upsert(testTrip(id = "trip-1", memberId = null))
            val remoteTrip = testTrip(id = "trip-1", memberId = "member-remote", updatedAt = FIXED_NOW)
            remoteReader.documents["trip-1"] = FirestoreDocument(fields = TripFirestoreMapper.toFields(remoteTrip))

            val result = coordinator.sync()

            assertEquals(listOf("trip-1"), result.succeeded.map { it.documentId })
            assertEquals(null, local.getById("trip-1")?.memberId)
        }

    @Test
    fun `no remote document - local is pushed`() =
        runTest {
            repository.upsert(testTrip(id = "trip-1"))

            val result = coordinator.sync()

            assertEquals(listOf("trip-1"), result.succeeded.map { it.documentId })
            assertEquals(listOf("trip-1"), remoteReader.readDocumentIds)
        }

    // --- Preflight and DELETE edge cases ---

    @Test
    fun `unreachable preflight keeps the operation queued and pushes nothing for it`() =
        runTest {
            repository.upsert(testTrip(id = "trip-1"))
            remoteReader.unreachable = true

            coordinator.sync()

            assertEquals(emptyList(), networkClient.pushed)
            val remaining = store.loadAll().single()
            assertEquals(PersistedSyncOperationStatus.RETRY, remaining.status)
            assertEquals(SyncStatus.PENDING, local.getById("trip-1")?.syncStatus)
        }

    @Test
    fun `delete operations skip the preflight read entirely`() =
        runTest {
            repository.upsert(testTrip(id = "trip-1"))
            coordinator.sync()
            remoteReader.readDocumentIds.clear()

            repository.delete("trip-1")
            remoteReader.unreachable = true
            val result = coordinator.sync()

            assertEquals(emptyList(), remoteReader.readDocumentIds)
            assertEquals(listOf("trip-1"), result.succeeded.map { it.documentId })
            assertTrue(store.loadAll().isEmpty())
        }
}
