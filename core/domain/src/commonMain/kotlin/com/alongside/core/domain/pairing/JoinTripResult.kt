package com.alongside.core.domain.pairing

import com.alongside.core.model.trip.Trip

/** Outcome of attempting to join a trip by invite code. */
public sealed interface JoinTripResult {
    /** The join succeeded; [trip] already carries the joiner as [Trip.memberId]. */
    public data class Joined(
        val trip: Trip,
    ) : JoinTripResult

    /** Malformed code, or no trip exists with this code. */
    public data object InvalidCode : JoinTripResult

    /** The joining user owns the trip with this code. */
    public data object OwnCode : JoinTripResult

    /** Another user already joined the trip with this code. */
    public data object AlreadyUsed : JoinTripResult
}
