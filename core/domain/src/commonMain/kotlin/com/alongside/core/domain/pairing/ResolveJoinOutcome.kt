package com.alongside.core.domain.pairing

import com.alongside.core.model.trip.Trip

/**
 * Pure join-validation rule: decides what joining [candidate] (the trip looked up by [code],
 * null when the lookup found nothing) means for [userId].
 */
public fun resolveJoinOutcome(
    code: String,
    userId: String,
    candidate: Trip?,
): JoinTripResult =
    when {
        !isValidInviteCodeFormat(code) -> JoinTripResult.InvalidCode
        candidate == null -> JoinTripResult.InvalidCode
        candidate.ownerId == userId -> JoinTripResult.OwnCode
        candidate.memberId == userId -> JoinTripResult.Joined(candidate)
        candidate.memberId != null -> JoinTripResult.AlreadyUsed
        else -> JoinTripResult.Joined(candidate.copy(memberId = userId))
    }
