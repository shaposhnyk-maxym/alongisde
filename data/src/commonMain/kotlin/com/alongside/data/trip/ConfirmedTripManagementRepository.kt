package com.alongside.data.trip

import com.alongside.core.domain.trip.DeleteTripResult
import com.alongside.core.domain.trip.LeaveTripResult
import com.alongside.core.domain.trip.TripManagementRepository
import com.alongside.data.sync.SyncCoordinator

/**
 * Decorates [delegate]'s authorization/mutation logic with a synchronous wait for Firestore
 * confirmation. Leave/Delete Trip is a deliberate, infrequent action - blocking on the network
 * (the Settings screen shows a spinner meanwhile) is the right tradeoff here, unlike the
 * offline-first fire-and-forget writes used elsewhere in the app: the whole point is to only
 * navigate away once the trip is actually gone remotely, not just queued to become so.
 */
public class ConfirmedTripManagementRepository(
    private val delegate: TripManagementRepository,
    private val syncCoordinator: SyncCoordinator,
) : TripManagementRepository {
    override suspend fun deleteTrip(
        tripId: String,
        callerId: String,
    ): DeleteTripResult {
        val result = delegate.deleteTrip(tripId, callerId)
        if (result != DeleteTripResult.Deleted) return result
        return if (awaitConfirmed(tripId)) DeleteTripResult.Deleted else DeleteTripResult.SyncFailed
    }

    override suspend fun leaveTrip(
        tripId: String,
        callerId: String,
    ): LeaveTripResult {
        val result = delegate.leaveTrip(tripId, callerId)
        if (result == LeaveTripResult.NotFound) return result
        return if (awaitConfirmed(tripId)) result else LeaveTripResult.SyncFailed
    }

    // A durable operation not reported as failed just now either succeeded this call or was
    // already confirmed by an earlier one - either way there's nothing left blocking on the
    // remote for this trip.
    private suspend fun awaitConfirmed(tripId: String): Boolean {
        val outcome = syncCoordinator.sync()
        return outcome.failed.none { it.documentId == tripId }
    }
}
