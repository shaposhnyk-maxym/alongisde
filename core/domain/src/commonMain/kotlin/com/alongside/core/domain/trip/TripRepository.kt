package com.alongside.core.domain.trip

import com.alongside.core.model.trip.Trip
import kotlinx.coroutines.flow.Flow

public interface TripRepository {
    public suspend fun upsert(trip: Trip)

    /**
     * Like [upsert], but always wins any remote conflict - for deliberate, user-confirmed
     * changes (Leave Trip, ownership transfer) where local intent must never be silently
     * overridden by a concurrent write, unlike routine offline-first upserts.
     */
    public suspend fun forceUpsert(trip: Trip)

    public suspend fun getById(id: String): Trip?

    public fun observeById(id: String): Flow<Trip?>

    public suspend fun delete(id: String)
}
