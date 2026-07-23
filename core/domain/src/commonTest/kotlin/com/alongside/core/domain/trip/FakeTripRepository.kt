package com.alongside.core.domain.trip

import com.alongside.core.model.trip.Trip
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** Fake data source recording every call, so tests can assert what the repository did. */
internal class FakeTripRepository : TripRepository {
    private val trips = MutableStateFlow<Map<String, Trip>>(emptyMap())

    val upserted = mutableListOf<Trip>()
    val deletedIds = mutableListOf<String>()

    fun seed(trip: Trip) {
        trips.update { it + (trip.id to trip) }
    }

    override suspend fun upsert(trip: Trip) {
        upserted += trip
        trips.update { it + (trip.id to trip) }
    }

    override suspend fun getById(id: String): Trip? = trips.value[id]

    override fun observeById(id: String): Flow<Trip?> = trips.map { it[id] }

    override suspend fun delete(id: String) {
        deletedIds += id
        trips.update { it - id }
    }
}
