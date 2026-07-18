package com.alongside.data.pairing

import com.alongside.core.domain.pairing.PairingTripDataSource
import com.alongside.core.domain.trip.TripRepository
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
            val poller =
                launch {
                    while (isActive) {
                        refreshFromRemote(userId)
                        delay(pollInterval)
                    }
                }
            launch { localLookup.observeByUserId(userId).collect { send(it) } }
            awaitClose { poller.cancel() }
        }

    override suspend fun save(trip: Trip) {
        trips.upsert(trip)
        try {
            syncCoordinator.sync()
        } catch (_: FirestoreException) {
            // Best-effort push; the operation is durably queued for the next sync.
        }
    }

    private suspend fun refreshFromRemote(userId: String) {
        try {
            remote.findTripByUserId(userId)?.also { cacheRemote(it) }
        } catch (_: FirestoreException) {
            // Poll again next tick; the local flow keeps emitting meanwhile.
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
