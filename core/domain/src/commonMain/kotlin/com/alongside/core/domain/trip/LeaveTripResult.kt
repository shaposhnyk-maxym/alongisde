package com.alongside.core.domain.trip

import com.alongside.core.model.trip.Trip

/** Outcome of attempting to leave a trip. */
public sealed interface LeaveTripResult {
    /** The member left; [trip] is the owner's, unchanged apart from a cleared [Trip.memberId]. */
    public data class Left(
        val trip: Trip,
    ) : LeaveTripResult

    /** The owner left a trip with a member present; ownership moved to them. */
    public data class OwnershipTransferred(
        val trip: Trip,
    ) : LeaveTripResult

    /** The owner left a still-solo trip - nothing to preserve, so it was deleted outright. */
    public data object Deleted : LeaveTripResult

    /** No trip exists with this id, or the caller isn't its owner or member. */
    public data object NotFound : LeaveTripResult

    /** Authorized and recorded, but couldn't be confirmed against the remote right now. */
    public data object SyncFailed : LeaveTripResult
}
