package com.alongside.data.trip

import com.alongside.core.domain.trip.DeleteTripResult
import com.alongside.core.domain.trip.LeaveTripResult
import com.alongside.core.domain.trip.TripManagementRepository
import com.alongside.core.domain.trip.TripRepository
import com.alongside.data.sync.SyncCoordinator

/**
 * Decorates [delegate]'s authorization/mutation logic with a synchronous wait for Firestore
 * confirmation. Leave/Delete Trip is a deliberate, infrequent action - blocking on the network
 * (the Settings screen shows a spinner meanwhile) is the right tradeoff here, unlike the
 * offline-first fire-and-forget writes used elsewhere in the app: the whole point is to only
 * navigate away once the trip is actually gone remotely, not just queued to become so.
 *
 * Confirmation is checked by re-reading [tripRepository] after [syncCoordinator] runs, not by
 * inspecting its [com.alongside.core.network.queue.SyncBatchResult] succeeded/failed lists.
 * SyncCoordinator's preflight conflict check for an UPSERT (Leave, or an ownership transfer) can
 * decide the remote is newer, silently apply it over the local write, and drop the operation from
 * the queue - that operation never reaches the succeeded/failed lists at all, so a caller that
 * only looked there would report the leave as confirmed even though it had just been reverted
 * back to the pre-leave state underneath it. Confirmed the hard way: on a real device the member
 * who "left" was routed to Home again instead of the Pairing choice screen, because the trip's
 * memberId had been silently restored.
 */
public class ConfirmedTripManagementRepository(
    private val delegate: TripManagementRepository,
    private val syncCoordinator: SyncCoordinator,
    private val tripRepository: TripRepository,
) : TripManagementRepository {
    override suspend fun deleteTrip(
        tripId: String,
        callerId: String,
    ): DeleteTripResult {
        val result = delegate.deleteTrip(tripId, callerId)
        if (result != DeleteTripResult.Deleted) return result
        // DELETE-type operations always PUSH unconditionally (SyncCoordinator.preflight has no
        // conflict-check branch for them), so an explicit push failure is the only way this
        // doesn't stick - no local-state re-verification needed the way leaveTrip's UPSERT does.
        val outcome = syncCoordinator.sync()
        val pushFailed = outcome.failed.any { it.documentId == tripId }
        return if (pushFailed) DeleteTripResult.SyncFailed else DeleteTripResult.Deleted
    }

    override suspend fun leaveTrip(
        tripId: String,
        callerId: String,
    ): LeaveTripResult {
        val result = delegate.leaveTrip(tripId, callerId)
        if (result == LeaveTripResult.NotFound) return result
        val outcome = syncCoordinator.sync()
        val pushFailed = outcome.failed.any { it.documentId == tripId }
        // A push not reported as failed doesn't guarantee it actually stuck: leaving is an
        // UPSERT, and SyncCoordinator's preflight conflict check can decide a concurrent remote
        // write is newer, silently apply it over the local write via applyRemote, and drop the
        // operation from the queue - that op never reaches succeeded/failed at all. Re-read the
        // persisted state and check it actually reflects what [result] claims before trusting it.
        val current = tripRepository.getById(tripId)
        val confirmed =
            !pushFailed &&
                when (result) {
                    is LeaveTripResult.Left -> current != null && current.memberId != callerId
                    is LeaveTripResult.OwnershipTransferred -> current != null && current.ownerId != callerId
                    LeaveTripResult.Deleted -> current == null
                    LeaveTripResult.NotFound, LeaveTripResult.SyncFailed -> true // unreachable
                }
        return if (confirmed) result else LeaveTripResult.SyncFailed
    }
}
