package com.alongside.core.domain.pairing

import com.alongside.core.model.trip.Trip
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * In-memory [PairingTripDataSource] — the runtime default until M9's `data` module provides
 * the real Room/Firestore-backed one, and the fake for tests (precedent: `InMemorySyncQueue`).
 */
public class InMemoryPairingTripDataSource : PairingTripDataSource {
    private val trips = MutableStateFlow<Map<String, Trip>>(emptyMap())

    override suspend fun findByInviteCode(code: String): Trip? = trips.value.values.firstOrNull { it.inviteCode == code }

    override fun observeByUserId(userId: String): Flow<Trip?> =
        trips.map { stored ->
            stored.values.firstOrNull { it.ownerId == userId || it.memberId == userId }
        }

    override suspend fun save(trip: Trip) {
        trips.update { it + (trip.id to trip) }
    }

    /** Test helper: all stored trips, outside the interface. */
    public fun snapshot(): List<Trip> = trips.value.values.toList()
}
