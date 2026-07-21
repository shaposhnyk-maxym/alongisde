package com.alongside.data

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.alongside.core.database.AlongsideDatabase
import com.alongside.core.database.sync.PersistedSyncOperationStatus
import com.alongside.core.database.syncOperationStore
import com.alongside.core.database.tripRepository
import com.alongside.core.model.SyncStatus
import com.alongside.core.network.firestore.model.FirestoreValue
import com.alongside.core.network.queue.MaxAttemptsRetryPolicy
import com.alongside.core.network.queue.SyncOperationType
import com.alongside.core.network.queue.SyncQueueProcessor
import com.alongside.data.FakeBackgroundWorkScheduler
import com.alongside.data.sync.FakeRemoteDocumentReader
import com.alongside.data.sync.RecordingSyncNetworkClient
import com.alongside.data.sync.SyncCoordinator
import com.alongside.data.trip.SyncingTripRepository
import com.alongside.data.trip.TripFirestoreMapper
import com.alongside.data.trip.TripSyncEntityBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

private val FIXED_NOW = Instant.fromEpochMilliseconds(1_752_800_000_000)

private object FixedClock : Clock {
    override fun now(): Instant = FIXED_NOW
}

/**
 * M9 accept criterion 1, on the real stack: Room-backed repository and store, real
 * coordinator/processor - only the network is a recording fake.
 */
class OfflineSyncIntegrationTest {
    private lateinit var database: AlongsideDatabase
    private lateinit var repository: SyncingTripRepository
    private lateinit var coordinator: SyncCoordinator
    private val networkClient = RecordingSyncNetworkClient()
    private val remoteReader = FakeRemoteDocumentReader()

    @BeforeTest
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder<AlongsideDatabase>()
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
        val local = database.tripRepository()
        repository =
            SyncingTripRepository(
                local = local,
                store = database.syncOperationStore(),
                backgroundWorkScheduler = FakeBackgroundWorkScheduler(),
                clock = FixedClock,
                generateOpId = { "op-${nextOpId++}" },
            )
        coordinator =
            SyncCoordinator(
                store = database.syncOperationStore(),
                processor = SyncQueueProcessor(networkClient, MaxAttemptsRetryPolicy(2)),
                remoteReader = remoteReader,
                bindings = listOf(TripSyncEntityBinding(local)),
            )
    }

    private var nextOpId = 1

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun `offline write lands only in Room then a sync after the network appears pushes it remotely`() =
        runTest {
            // Offline: the write happens, no sync runs.
            repository.upsert(testTrip(id = "trip-1", memberId = "member-1"))

            assertEquals(emptyList(), networkClient.pushed)
            val stored = repository.getById("trip-1")
            assertEquals(SyncStatus.PENDING, stored?.syncStatus)
            assertEquals(FIXED_NOW, stored?.updatedAt)
            val queued = database.syncOperationStore().loadAll().single()
            assertEquals(PersistedSyncOperationStatus.PENDING, queued.status)

            // The network appears: the queue drains into the recording client.
            val result = coordinator.sync()

            assertEquals(1, result.succeeded.size)
            val pushed = networkClient.pushed.single()
            assertEquals(SyncOperationType.UPSERT, pushed.type)
            assertEquals("trips", pushed.collectionPath)
            assertEquals("trip-1", pushed.documentId)
            assertEquals(FirestoreValue.StringValue("member-1"), pushed.fields["memberId"])
            val expected = repository.getById("trip-1")!!
            assertEquals(TripFirestoreMapper.toFields(expected), pushed.fields)

            assertEquals(SyncStatus.SYNCED, repository.getById("trip-1")?.syncStatus)
            assertTrue(database.syncOperationStore().loadAll().isEmpty())

            // A second sync has nothing left to push.
            coordinator.sync()
            assertEquals(1, networkClient.pushed.size)
        }

    @Test
    fun `sync attempted while offline keeps the operation queued until connectivity returns`() =
        runTest {
            repository.upsert(testTrip(id = "trip-1"))
            networkClient.failAll = true
            remoteReader.unreachable = true

            coordinator.sync()

            assertEquals(emptyList(), networkClient.pushed)
            val stalled =
                database
                    .syncOperationStore()
                    .loadAll()
                    .single()
            assertEquals(PersistedSyncOperationStatus.RETRY, stalled.status)
            assertEquals(SyncStatus.PENDING, repository.getById("trip-1")?.syncStatus)

            networkClient.failAll = false
            remoteReader.unreachable = false
            val result = coordinator.sync()

            assertEquals(1, result.succeeded.size)
            assertEquals(SyncStatus.SYNCED, repository.getById("trip-1")?.syncStatus)
            assertTrue(database.syncOperationStore().loadAll().isEmpty())
        }
}
