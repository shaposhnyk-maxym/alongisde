package com.alongside.core.domain.pairing

import com.alongside.core.model.trip.Trip
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

public interface PairingRepository {
    /** Creates a trip owned by [ownerId] with a freshly generated, unique invite code. */
    public suspend fun createTrip(
        ownerId: String,
        startDate: LocalDate,
        endDate: LocalDate,
    ): Trip

    public suspend fun joinTrip(
        code: String,
        userId: String,
    ): JoinTripResult

    /** The trip [userId] participates in (as owner or member), null while unpaired. */
    public fun observeActiveTrip(userId: String): Flow<Trip?>
}
