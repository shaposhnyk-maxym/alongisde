package com.alongside.data.pairing

import com.alongside.core.database.sync.PersistedSyncOperationStatus
import com.alongside.core.model.SyncStatus
import com.alongside.core.model.trip.Trip
import com.alongside.core.network.queue.MaxAttemptsRetryPolicy
import com.alongside.core.network.queue.SyncQueueProcessor
import com.alongside.data.FakeBackgroundWorkScheduler
import com.alongside.data.sync.FakeRemoteDocumentReader
import com.alongside.data.sync.InMemorySyncOperationStore
import com.alongside.data.sync.RecordingSyncNetworkClient
import com.alongside.data.sync.SyncCoordinator
import com.alongside.data.testTrip
import com.alongside.data.trip.RecordingTripRepository
import com.alongside.data.trip.SyncingTripRepository
import com.alongside.data.trip.TripSyncEntityBinding
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

private val FIXED_NOW = Instant.fromEpochMilliseconds(1_752_800_000_000)
private val POLL_INTERVAL = 5.seconds

private object FixedClock : Clock {
    override fun now(): Instant = FIXED_NOW
}

class FirestorePairingTripDataSourceTest {
    private val local = RecordingTripRepository()
    private val store = InMemorySyncOperationStore()
    private val networkClient = RecordingSyncNetworkClient()
    private val remote = FakePairingRemoteDataSource()
    private var nextOpId = 0
    private val syncingTrips =
        SyncingTripRepository(
            local = local,
            store = store,
            backgroundWorkScheduler = FakeBackgroundWorkScheduler(),
            clock = FixedClock,
            generateOpId = { "op-${++nextOpId}" },
        )
    private val dataSource =
        FirestorePairingTripDataSource(
            trips = syncingTrips,
            localLookup = local,
            remote = remote,
            syncCoordinator =
                SyncCoordinator(
                    store = store,
                    processor = SyncQueueProcessor(networkClient, MaxAttemptsRetryPolicy(2)),
                    remoteReader = FakeRemoteDocumentReader(),
                    bindings = listOf(TripSyncEntityBinding(local)),
                ),
            pollInterval = POLL_INTERVAL,
        )

    // --- findByInviteCode ---

    @Test
    fun `remote invite-code hit is returned and cached into the local store`() =
        runTest {
            val remoteTrip = testTrip(id = "trip-r", syncStatus = SyncStatus.SYNCED)
            remote.tripsByInviteCode["ABCD23"] = remoteTrip

            val found = dataSource.findByInviteCode("ABCD23")

            assertEquals(remoteTrip, found)
            assertEquals(remoteTrip, local.getById("trip-r"))
            assertEquals(listOf(remoteTrip), local.savedDirectly)
        }

    @Test
    fun `remote outage falls back to the local invite-code lookup`() =
        runTest {
            val cached = testTrip(id = "trip-l")
            local.save(cached)
            remote.unreachable = true

            assertEquals(cached, dataSource.findByInviteCode("ABCD23"))
        }

    @Test
    fun `unknown code returns null without an exception`() =
        runTest {
            assertNull(dataSource.findByInviteCode("XXXX99"))
        }

    @Test
    fun `a newer local copy is not overwritten by a stale remote hit`() =
        runTest {
            val localCopy = testTrip(id = "trip-1", memberId = "member-local", updatedAt = FIXED_NOW)
            local.save(localCopy)
            remote.tripsByInviteCode["ABCD23"] =
                testTrip(id = "trip-1", memberId = null, updatedAt = FIXED_NOW - 1.minutes)

            dataSource.findByInviteCode("ABCD23")

            assertEquals(localCopy, local.getById("trip-1"))
        }

    // --- observeByUserId: the waiting-owner flow ---

    @Test
    fun `observeByUserId emits the local trip immediately and the partner join after a poll tick`() =
        runTest {
            val created = testTrip(id = "trip-1", ownerId = "owner-1", memberId = null)
            local.save(created)
            val emissions = mutableListOf<Trip?>()
            val collector = launch { dataSource.observeByUserId("owner-1").collect { emissions += it } }
            runCurrent()

            assertEquals(created, emissions.last())

            // The partner joins remotely with a newer timestamp; the next poll picks it up.
            val joined = created.copy(memberId = "member-1", updatedAt = FIXED_NOW + 1.minutes)
            remote.tripsByUserId["owner-1"] = joined
            advanceTimeBy(POLL_INTERVAL)
            runCurrent()

            assertEquals(joined, emissions.last())
            collector.cancel()
        }

    @Test
    fun `observeByUserId drains operations parked in the durable queue on each poll tick`() =
        runTest {
            // A write whose push never happened (e.g. it 403'd before auth was wired):
            // upsert enqueues durably but only save()/the poller actually push.
            syncingTrips.upsert(testTrip(id = "trip-1", ownerId = "owner-1"))
            assertEquals(1, store.loadAll().size)

            val emissions = mutableListOf<Trip?>()
            val collector = launch { dataSource.observeByUserId("owner-1").collect { emissions += it } }
            runCurrent()

            assertEquals(listOf("trip-1"), networkClient.pushed.map { it.documentId })
            assertTrue(store.loadAll().isEmpty())
            assertEquals(SyncStatus.SYNCED, local.getById("trip-1")?.syncStatus)
            collector.cancel()
        }

    @Test
    fun `polling failures keep the local flow alive`() =
        runTest {
            val created = testTrip(id = "trip-1", ownerId = "owner-1")
            local.save(created)
            remote.unreachable = true
            val emissions = mutableListOf<Trip?>()
            val collector = launch { dataSource.observeByUserId("owner-1").collect { emissions += it } }
            runCurrent()
            advanceTimeBy(POLL_INTERVAL * 3)
            runCurrent()

            assertEquals(created, emissions.last())
            assertTrue(remote.userIdLookups >= 3)
            collector.cancel()
        }

    // --- getActiveTrip: the one-shot Worker path, no long-lived poller warming Room first ---

    @Test
    fun `getActiveTrip returns the local copy without touching remote when Room already has it`() =
        runTest {
            local.save(testTrip(id = "trip-1", ownerId = "owner-1"))

            val found = dataSource.getActiveTrip("owner-1")

            assertEquals("trip-1", found?.id)
            assertEquals(0, remote.userIdLookups)
        }

    @Test
    fun `getActiveTrip falls back to remote and caches it when the local cache is empty`() =
        runTest {
            // The scenario that broke retryAllIncompleteEpisodes/retryAllIncompletePlaces: a
            // fresh install/local-data wipe leaves Room empty with no poller having run yet.
            val remoteTrip = testTrip(id = "trip-r", ownerId = "owner-1")
            remote.tripsByUserId["owner-1"] = remoteTrip

            val found = dataSource.getActiveTrip("owner-1")

            assertEquals(remoteTrip, found)
            assertEquals(remoteTrip, local.getById("trip-r"))
        }

    @Test
    fun `getActiveTrip returns null when both local and remote have nothing`() =
        runTest {
            assertNull(dataSource.getActiveTrip("owner-1"))
        }

    @Test
    fun `getActiveTrip swallows a remote outage and returns null instead of throwing`() =
        runTest {
            remote.unreachable = true

            assertNull(dataSource.getActiveTrip("owner-1"))
        }

    // --- save ---

    @Test
    fun `save stamps the trip enqueues it durably and pushes best-effort`() =
        runTest {
            dataSource.save(testTrip(id = "trip-1"))

            val pushed = networkClient.pushed.single()
            assertEquals("trip-1", pushed.documentId)
            assertTrue(store.loadAll().isEmpty())
            assertEquals(SyncStatus.SYNCED, local.getById("trip-1")?.syncStatus)
            assertEquals(FIXED_NOW, local.getById("trip-1")?.updatedAt)
        }

    @Test
    fun `save survives a failing push and leaves the operation queued for retry`() =
        runTest {
            networkClient.failAll = true

            dataSource.save(testTrip(id = "trip-1"))

            assertEquals(PersistedSyncOperationStatus.RETRY, store.loadAll().single().status)
            assertEquals(SyncStatus.FAILED, local.getById("trip-1")?.syncStatus)

            networkClient.failAll = false
            dataSource.save(testTrip(id = "trip-2", inviteCode = "CDEF23"))

            assertTrue(store.loadAll().isEmpty())
            assertEquals(SyncStatus.SYNCED, local.getById("trip-1")?.syncStatus)
        }
}
