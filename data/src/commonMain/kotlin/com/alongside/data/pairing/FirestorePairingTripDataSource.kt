package com.alongside.data.pairing

import com.alongside.core.domain.pairing.PairingTripDataSource
import com.alongside.core.domain.trip.TripRepository
import com.alongside.core.model.SyncStatus
import com.alongside.core.model.trip.Trip
import com.alongside.core.network.firestore.FirestoreException
import com.alongside.data.sync.ConflictWinner
import com.alongside.data.sync.SyncCoordinator
import com.alongside.data.sync.resolveConflict
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * The real (M9) [PairingTripDataSource]: Room is the source of truth the UI observes;
 * the remote is consulted for invite-code lookups and polled while someone is watching
 * [observeByUserId] - that's how the waiting owner sees the partner join. Remote failures
 * never break the flow; every remote read is cached into Room via last-write-wins.
 *
 * [trips] must be the syncing repository (it stamps `updatedAt` and enqueues the durable
 * sync operation); [localLookup] is the plain Room-backed lookup over the same table.
 */
public class FirestorePairingTripDataSource(
    private val trips: TripRepository,
    private val localLookup: PairingTripDataSource,
    private val remote: PairingRemoteDataSource,
    private val syncCoordinator: SyncCoordinator,
    private val pollInterval: Duration = DEFAULT_POLL_INTERVAL,
) : PairingTripDataSource {
    override suspend fun findByInviteCode(code: String): Trip? =
        try {
            val remoteTrip = remote.findTripByInviteCode(code)
            remoteTrip?.also { cacheRemote(it) } ?: localLookup.findByInviteCode(code)
        } catch (_: FirestoreException) {
            // Offline invite-code lookups must still work: createTrip uses this
            // for uniqueness checks and join can match a locally cached trip.
            localLookup.findByInviteCode(code)
        }

    override fun observeByUserId(userId: String): Flow<Trip?> =
        channelFlow {
            // Remembered ACROSS ticks, not just within one: the local delete behind a
            // Leave/Delete Trip action happens whenever the user confirms it, with no
            // synchronization to any particular poller tick - by the time a tick notices the
            // local row is gone, it may have disappeared several ticks ago already. Once that's
            // observed, its id stays suppressed (ignored if a lagging remote read still shows
            // it) until local has something again, not just for the one tick it was noticed on.
            var lastKnownTripId: String? = null
            var suppressedId: String? = null
            val poller =
                launch {
                    while (isActive) {
                        val current = localLookup.getActiveTrip(userId)
                        if (current != null) {
                            lastKnownTripId = current.id
                            suppressedId = null
                        } else if (lastKnownTripId != null) {
                            suppressedId = lastKnownTripId
                            lastKnownTripId = null
                        }
                        // Snapshotted BEFORE pushPendingSync(), not after: a trip pushed for the
                        // first time this very tick flips PENDING -> SYNCED locally, but Firestore
                        // may not yet answer a findTripByUserId query with what was just written
                        // (no same-tick read-after-write guarantee) - refreshFromRemote seeing an
                        // empty remote a moment later must not mistake that lag for a delete.
                        val previouslySynced = current?.takeIf { it.syncStatus == SyncStatus.SYNCED }
                        // Push before pull: operations parked in the durable queue (a write
                        // that 403'd or happened offline) have no other trigger than save() -
                        // the poll tick is what drains them once connectivity/auth is back.
                        pushPendingSync()
                        refreshFromRemote(userId, previouslySynced, suppressedId)
                        delay(pollInterval)
                    }
                }
            launch { localLookup.observeByUserId(userId).collect { send(it) } }
            awaitClose { poller.cancel() }
        }

    // Local-first, remote-fallback only on an empty cache - the opposite order from
    // findByInviteCode. This is a one-shot call with no long-lived poller warming Room first
    // (e.g. a Worker right after a fresh install/local-data wipe), so an empty local cache here
    // must not be mistaken for "genuinely unpaired" - it may just not have synced yet.
    override suspend fun getActiveTrip(userId: String): Trip? = localLookup.getActiveTrip(userId) ?: fromRemote(userId)

    private suspend fun fromRemote(userId: String): Trip? =
        try {
            remote.findTripByUserId(userId)?.also { cacheRemote(it) }
        } catch (e: FirestoreException) {
            println("FirestorePairingTripDataSource: getActiveTrip fallback threw ${e::class.simpleName}: ${e.message}")
            null
        }

    override suspend fun save(trip: Trip) {
        trips.upsert(trip)
        pushPendingSync()
    }

    override suspend fun delete(tripId: String) {
        localLookup.delete(tripId)
    }

    // catch(Throwable) is deliberate here, not an oversight: this is a poller's per-tick sync
    // call, and an unexpected (non-FirestoreException) failure must still be logged with a clear
    // "UNEXPECTED" marker before it's rethrown - swallowing it silently would make the poller die
    // without a trace, and narrowing the catch would lose that rethrow-after-logging behavior.
    @Suppress("TooGenericExceptionCaught")
    private suspend fun pushPendingSync() {
        try {
            syncCoordinator.sync()
        } catch (e: FirestoreException) {
            println("FirestorePairingTripDataSource: sync() threw ${e::class.simpleName}: ${e.message}")
        } catch (e: Throwable) {
            println("FirestorePairingTripDataSource: sync() threw UNEXPECTED ${e::class.simpleName}: ${e.message}")
            throw e
        }
    }

    // [previouslySynced] and [suppressedId] are both decided from local state as of BEFORE this
    // tick's pushPendingSync() - see the poller loop's comments for why.
    private suspend fun refreshFromRemote(
        userId: String,
        previouslySynced: Trip?,
        suppressedId: String?,
    ) {
        try {
            val remoteTrip = remote.findTripByUserId(userId)
            when {
                remoteTrip != null && remoteTrip.id == suppressedId -> Unit // stale echo of our own delete
                remoteTrip != null -> cacheRemote(remoteTrip)
                previouslySynced != null -> {
                    // Gone on the remote and was already confirmed synced before this tick -
                    // deleted by the other person, from their device. Without this the local
                    // cache stays stuck showing a trip that no longer exists anywhere, until
                    // app restart.
                    localLookup.delete(previouslySynced.id)
                }
            }
        } catch (e: FirestoreException) {
            val detail =
                when (e) {
                    is FirestoreException.ClientError -> "code=${e.code} status=${e.status} message=${e.message}"
                    is FirestoreException.ServerError -> "code=${e.code} status=${e.status} message=${e.message}"
                    else -> e.message.toString()
                }
            println("FirestorePairingTripDataSource: refreshFromRemote threw ${e::class.simpleName}: $detail")
        }
    }

    /** Writes through [localLookup], not [trips] - a cached remote copy must not re-enqueue sync work. */
    private suspend fun cacheRemote(remoteTrip: Trip) {
        val existing = trips.getById(remoteTrip.id)
        val winner = existing?.let { resolveConflict(it.updatedAt, remoteTrip.updatedAt) } ?: ConflictWinner.REMOTE
        if (winner == ConflictWinner.REMOTE) {
            localLookup.save(remoteTrip)
        }
    }

    private companion object {
        val DEFAULT_POLL_INTERVAL: Duration = 5.seconds
    }
}
