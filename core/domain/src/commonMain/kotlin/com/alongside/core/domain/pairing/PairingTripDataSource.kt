package com.alongside.core.domain.pairing

import com.alongside.core.model.trip.Trip
import kotlinx.coroutines.flow.Flow

/**
 * Narrow trip-access seam for pairing. Deliberately separate from the id-keyed
 * `TripRepository`: pairing needs invite-code and user-scoped lookups that the real
 * data layer (M9) will back with Firestore queries.
 */
public interface PairingTripDataSource {
    public suspend fun findByInviteCode(code: String): Trip?

    /** The trip where [userId] is the owner or the joined member, null when there is none. */
    public fun observeByUserId(userId: String): Flow<Trip?>

    public suspend fun save(trip: Trip)
}
