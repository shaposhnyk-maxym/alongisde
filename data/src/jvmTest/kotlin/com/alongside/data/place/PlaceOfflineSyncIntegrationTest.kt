package com.alongside.data.place

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.alongside.core.database.AlongsideDatabase
import com.alongside.core.database.placeCandidateRepository
import com.alongside.core.database.sync.PersistedSyncOperationStatus
import com.alongside.core.database.syncOperationStore
import com.alongside.core.model.SyncStatus
import com.alongside.core.model.place.SwipeDirection
import com.alongside.core.network.firestore.model.FirestoreValue
import com.alongside.core.network.queue.MaxAttemptsRetryPolicy
import com.alongside.core.network.queue.SyncOperationType
import com.alongside.core.network.queue.SyncQueueProcessor
import com.alongside.data.sync.FakeRemoteDocumentReader
import com.alongside.data.sync.RecordingSyncNetworkClient
import com.alongside.data.sync.SyncCoordinator
import com.alongside.data.testPlace
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

private object OfflineSyncClock : Clock {
    override fun now(): Instant = FIXED_NOW
}

/**
 * PlaceCandidate's equivalent of `OfflineSyncIntegrationTest` (Trip's, M9) - on the real stack:
 * Room-backed repository and store, real coordinator/processor, only the network is a recording
 * fake. The generic LWW/partial-failure behavior itself is already fully proven entity-agnostic
 * by M9's `SyncCoordinatorTest`/`ResolveConflictTest` - not re-derived here for a fourth entity.
 */
class PlaceOfflineSyncIntegrationTest {
    private lateinit var database: AlongsideDatabase
    private lateinit var repository: SyncingPlaceCandidateRepository
    private lateinit var coordinator: SyncCoordinator
    private val networkClient = RecordingSyncNetworkClient()
    private val remoteReader = FakeRemoteDocumentReader()
    private var nextOpId = 1

    @BeforeTest
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder<AlongsideDatabase>()
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
        val local = database.placeCandidateRepository()
        repository =
            SyncingPlaceCandidateRepository(
                local = local,
                store = database.syncOperationStore(),
                clock = OfflineSyncClock,
                generateOpId = { "op-${nextOpId++}" },
            )
        coordinator =
            SyncCoordinator(
                store = database.syncOperationStore(),
                processor = SyncQueueProcessor(networkClient, MaxAttemptsRetryPolicy(2)),
                remoteReader = remoteReader,
                bindings = listOf(PlaceCandidateSyncEntityBinding(local)),
            )
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun `offline write lands only in Room then a sync after the network appears pushes it remotely`() =
        runTest {
            repository.upsert(testPlace(id = "place-1", ownerSwipe = SwipeDirection.LIKE))

            assertEquals(emptyList(), networkClient.pushed)
            val stored = repository.getById("place-1")
            assertEquals(SyncStatus.PENDING, stored?.syncStatus)
            assertEquals(FIXED_NOW, stored?.updatedAt)
            val queued = database.syncOperationStore().loadAll().single()
            assertEquals(PersistedSyncOperationStatus.PENDING, queued.status)

            val result = coordinator.sync()

            assertEquals(1, result.succeeded.size)
            val pushed = networkClient.pushed.single()
            assertEquals(SyncOperationType.UPSERT, pushed.type)
            assertEquals("placeCandidates", pushed.collectionPath)
            assertEquals("place-1", pushed.documentId)
            assertEquals(FirestoreValue.StringValue("LIKE"), pushed.fields["ownerSwipe"])
            val expected = repository.getById("place-1")!!
            assertEquals(PlaceCandidateFirestoreMapper.toFields(expected), pushed.fields)

            assertEquals(SyncStatus.SYNCED, repository.getById("place-1")?.syncStatus)
            assertTrue(database.syncOperationStore().loadAll().isEmpty())

            coordinator.sync()
            assertEquals(1, networkClient.pushed.size)
        }

    @Test
    fun `sync attempted while offline keeps the operation queued until connectivity returns`() =
        runTest {
            repository.upsert(testPlace(id = "place-1"))
            networkClient.failAll = true
            remoteReader.unreachable = true

            coordinator.sync()

            assertEquals(emptyList(), networkClient.pushed)
            val stalled = database.syncOperationStore().loadAll().single()
            assertEquals(PersistedSyncOperationStatus.RETRY, stalled.status)
            assertEquals(SyncStatus.PENDING, repository.getById("place-1")?.syncStatus)

            networkClient.failAll = false
            remoteReader.unreachable = false
            val result = coordinator.sync()

            assertEquals(1, result.succeeded.size)
            assertEquals(SyncStatus.SYNCED, repository.getById("place-1")?.syncStatus)
            assertTrue(database.syncOperationStore().loadAll().isEmpty())
        }
}
