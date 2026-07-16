package com.alongside.core.domain.trip

import com.alongside.core.model.trip.Trip
import kotlinx.coroutines.flow.Flow

public interface TripRepository {
    public suspend fun upsert(trip: Trip)

    public suspend fun getById(id: String): Trip?

    public fun observeById(id: String): Flow<Trip?>

    public suspend fun delete(id: String)
}
