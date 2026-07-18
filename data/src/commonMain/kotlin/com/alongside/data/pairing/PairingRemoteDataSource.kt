package com.alongside.data.pairing

import com.alongside.core.model.trip.Trip

/** Remote side of pairing lookups - an interface, not HTTP, so tests script it directly. */
public interface PairingRemoteDataSource {
    public suspend fun findTripByInviteCode(code: String): Trip?

    /** The trip where [userId] is the owner or the joined member, null when there is none. */
    public suspend fun findTripByUserId(userId: String): Trip?
}
