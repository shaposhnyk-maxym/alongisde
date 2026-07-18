package com.alongside.data.trip

import com.alongside.core.domain.trip.TripRepository
import com.alongside.core.model.trip.Trip
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** Map-backed local [TripRepository] fake that records calls for assertions. */
internal class RecordingTripRepository : TripRepository {
    private val trips = MutableStateFlow<Map<String, Trip>>(emptyMap())
    val upserted = mutableListOf<Trip>()
    val deletedIds = mutableListOf<String>()

    override suspend fun upsert(trip: Trip) {
        upserted += trip
        trips.value = trips.value + (trip.id to trip)
    }

    override suspend fun getById(id: String): Trip? = trips.value[id]

    override fun observeById(id: String): Flow<Trip?> = trips.map { it[id] }

    override suspend fun delete(id: String) {
        deletedIds += id
        trips.value = trips.value - id
    }
}
