package com.alongside.data.trip

import com.alongside.core.domain.pairing.PairingTripDataSource
import com.alongside.core.domain.trip.TripRepository
import com.alongside.core.model.trip.Trip
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Map-backed local trips fake that records calls for assertions. Implements both local
 * seams over the one map, the way Room serves both from the one `trips` table.
 */
internal class RecordingTripRepository :
    TripRepository,
    PairingTripDataSource {
    private val trips = MutableStateFlow<Map<String, Trip>>(emptyMap())
    val upserted = mutableListOf<Trip>()
    val deletedIds = mutableListOf<String>()
    val savedDirectly = mutableListOf<Trip>()

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

    override suspend fun findByInviteCode(code: String): Trip? = trips.value.values.firstOrNull { it.inviteCode == code }

    override fun observeByUserId(userId: String): Flow<Trip?> =
        trips.map { all -> all.values.firstOrNull { it.ownerId == userId || it.memberId == userId } }

    override suspend fun getActiveTrip(userId: String): Trip? =
        trips.value.values.firstOrNull { it.ownerId == userId || it.memberId == userId }

    override suspend fun save(trip: Trip) {
        savedDirectly += trip
        trips.value = trips.value + (trip.id to trip)
    }
}
