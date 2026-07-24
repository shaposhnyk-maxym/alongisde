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
import kotlinx.coroutines.test.TestScope
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

    // A function, not a class-level val: the shared poller's delay(pollInterval) must run on
    // this TestScope so advanceTimeBy/runCurrent can drive it - a scope built outside runTest
    // would use real wall-clock time instead.
    private fun TestScope.dataSource() =
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
            scope = backgroundScope,
        )

    // --- findByInviteCode ---

    @Test
    fun `remote invite-code hit is returned and cached into the local store`() =
        runTest {
            val dataSource = dataSource()
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
            val dataSource = dataSource()
            val cached = testTrip(id = "trip-l")
            local.save(cached)
            remote.unreachable = true

            assertEquals(cached, dataSource.findByInviteCode("ABCD23"))
        }

    @Test
    fun `unknown code returns null without an exception`() =
        runTest {
            val dataSource = dataSource()
            assertNull(dataSource.findByInviteCode("XXXX99"))
        }

    @Test
    fun `a newer local copy is not overwritten by a stale remote hit`() =
        runTest {
            val dataSource = dataSource()
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
            val dataSource = dataSource()
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
            val dataSource = dataSource()
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
    fun `observeByUserId clears a synced local trip the remote no longer has`() =
        runTest {
            val dataSource = dataSource()
            // The other person deleted (or left, transferring ownership away from) the trip -
            // remote has nothing for this user any more. Without this, the local cache would
            // stay stuck showing a trip that's gone everywhere else, until the app restarts.
            local.save(testTrip(id = "trip-1", ownerId = "owner-1", syncStatus = SyncStatus.SYNCED))
            val emissions = mutableListOf<Trip?>()
            val collector = launch { dataSource.observeByUserId("owner-1").collect { emissions += it } }
            runCurrent()

            // The poller's very first tick runs immediately (no initial delay) and races the
            // local-observer's own first emission under the test dispatcher's eager scheduling -
            // both orderings are valid here, only the settled end state matters.
            advanceTimeBy(POLL_INTERVAL)
            runCurrent()

            assertNull(emissions.last())
            assertEquals(listOf("trip-1"), local.deletedIds)
            collector.cancel()
        }

    @Test
    fun `observeByUserId does not clear a not-yet-synced local trip just because remote is empty`() =
        runTest {
            val dataSource = dataSource()
            // createTrip while offline: local is PENDING and remote genuinely has nothing yet -
            // that must not be mistaken for a remote delete.
            local.save(testTrip(id = "trip-1", ownerId = "owner-1", syncStatus = SyncStatus.PENDING))
            val emissions = mutableListOf<Trip?>()
            val collector = launch { dataSource.observeByUserId("owner-1").collect { emissions += it } }
            runCurrent()

            advanceTimeBy(POLL_INTERVAL)
            runCurrent()

            assertEquals("trip-1", emissions.last()?.id)
            assertEquals(emptyList(), local.deletedIds)
            collector.cancel()
        }

    @Test
    fun `deleting the trip locally is not undone by a stale remote read in the same poll tick`() =
        runTest {
            val dataSource = dataSource()
            // Own-device delete: local.delete() runs synchronously, but the durable DELETE
            // operation only gets *pushed* to Firestore inside this same tick's pushPendingSync()
            // - remote.tripsByUserId is a separate fake here (standing in for Firestore not yet
            // reflecting a write it was just sent), so the immediately-following remote read
            // must not resurrect the trip it was just told to delete.
            val trip = testTrip(id = "trip-1", ownerId = "owner-1", memberId = "member-1", syncStatus = SyncStatus.SYNCED)
            local.save(trip)
            remote.tripsByUserId["owner-1"] = trip
            val emissions = mutableListOf<Trip?>()
            val collector = launch { dataSource.observeByUserId("owner-1").collect { emissions += it } }
            runCurrent()

            syncingTrips.delete("trip-1")
            advanceTimeBy(POLL_INTERVAL)
            runCurrent()

            assertNull(emissions.last())
            assertNull(local.getById("trip-1"))
            collector.cancel()
        }

    @Test
    fun `polling failures keep the local flow alive`() =
        runTest {
            val dataSource = dataSource()
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

    @Test
    fun `two independent subscribers for the same userId share one poller instead of racing separate ones`() =
        runTest {
            // Regression test: PairingContainer and SettingsContainer both call
            // observeActiveTrip(uid) independently, and are both process-lifetime singletons
            // under Navigation 3 (no per-entry ViewModelStoreOwner). A cold channelFlow used to
            // hand each of them its OWN poller, so two pollers wrote the same Room row on
            // overlapping ticks and doubled the emissions each collector saw - which meant
            // PairingContainer's memberId-triggered Paired side effect could fire twice for one
            // real pairing, with the second, uncollected posting replaying later as a bogus
            // navigation. One shared poller means each tick produces at most one emission.
            val dataSource = dataSource()
            val created = testTrip(id = "trip-1", ownerId = "owner-1", memberId = null)
            local.save(created)
            val firstSubscriber = mutableListOf<Trip?>()
            val secondSubscriber = mutableListOf<Trip?>()
            val firstCollector = launch { dataSource.observeByUserId("owner-1").collect { firstSubscriber += it } }
            runCurrent()
            val secondCollector = launch { dataSource.observeByUserId("owner-1").collect { secondSubscriber += it } }
            runCurrent()

            val joined = created.copy(memberId = "member-1", updatedAt = FIXED_NOW + 1.minutes)
            remote.tripsByUserId["owner-1"] = joined
            advanceTimeBy(POLL_INTERVAL)
            runCurrent()

            // One poller x two ticks so far (subscribe + one advanceTimeBy) - two independent
            // pollers racing the same userId would have run 4 lookups between them by now.
            assertEquals(2, remote.userIdLookups)
            assertEquals(joined, firstSubscriber.last())
            assertEquals(joined, secondSubscriber.last())
            firstCollector.cancel()
            secondCollector.cancel()
        }

    // --- getActiveTrip: the one-shot Worker path, no long-lived poller warming Room first ---

    @Test
    fun `getActiveTrip returns the local copy without touching remote when Room already has it`() =
        runTest {
            val dataSource = dataSource()
            local.save(testTrip(id = "trip-1", ownerId = "owner-1"))

            val found = dataSource.getActiveTrip("owner-1")

            assertEquals("trip-1", found?.id)
            assertEquals(0, remote.userIdLookups)
        }

    @Test
    fun `getActiveTrip falls back to remote and caches it when the local cache is empty`() =
        runTest {
            val dataSource = dataSource()
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
            val dataSource = dataSource()
            assertNull(dataSource.getActiveTrip("owner-1"))
        }

    @Test
    fun `getActiveTrip swallows a remote outage and returns null instead of throwing`() =
        runTest {
            val dataSource = dataSource()
            remote.unreachable = true

            assertNull(dataSource.getActiveTrip("owner-1"))
        }

    // --- save ---

    @Test
    fun `save stamps the trip enqueues it durably and pushes best-effort`() =
        runTest {
            val dataSource = dataSource()
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
            val dataSource = dataSource()
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
