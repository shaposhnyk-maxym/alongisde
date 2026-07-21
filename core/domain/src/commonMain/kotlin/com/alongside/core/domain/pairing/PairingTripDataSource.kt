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

    /**
     * One-shot equivalent of [observeByUserId] - unlike collecting the first value off that
     * Flow, this is safe to call from a context with no long-lived observer already warming the
     * local cache (e.g. a background Worker right after a fresh install/local-data wipe): it
     * must not race a background poller and return a premature `null` just because nothing is
     * cached locally yet.
     */
    public suspend fun getActiveTrip(userId: String): Trip?

    public suspend fun save(trip: Trip)
}
