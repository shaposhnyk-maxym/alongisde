package com.alongside.feature.settings

import com.alongside.core.domain.trip.TripRepository
import com.alongside.core.model.trip.Trip
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** In-memory [TripRepository] fake driving the real [com.alongside.core.domain.trip.DefaultTripManagementRepository]. */
internal class InMemoryTripRepository : TripRepository {
    private val trips = MutableStateFlow<Map<String, Trip>>(emptyMap())

    override suspend fun upsert(trip: Trip) {
        trips.update { it + (trip.id to trip) }
    }

    override suspend fun forceUpsert(trip: Trip) {
        trips.update { it + (trip.id to trip) }
    }

    override suspend fun getById(id: String): Trip? = trips.value[id]

    override fun observeById(id: String): Flow<Trip?> = trips.map { it[id] }

    override suspend fun delete(id: String) {
        trips.update { it - id }
    }
}
