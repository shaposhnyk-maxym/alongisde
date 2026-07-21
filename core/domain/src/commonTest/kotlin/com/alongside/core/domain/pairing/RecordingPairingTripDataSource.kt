package com.alongside.core.domain.pairing

import com.alongside.core.model.trip.Trip
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** Fake data source recording every call, so tests can assert what the repository did. */
internal class RecordingPairingTripDataSource : PairingTripDataSource {
    private val trips = MutableStateFlow<Map<String, Trip>>(emptyMap())

    val savedTrips = mutableListOf<Trip>()
    val lookedUpCodes = mutableListOf<String>()

    override suspend fun findByInviteCode(code: String): Trip? {
        lookedUpCodes += code
        return trips.value.values.firstOrNull { it.inviteCode == code }
    }

    override fun observeByUserId(userId: String): Flow<Trip?> =
        trips.map { stored ->
            stored.values.firstOrNull { it.ownerId == userId || it.memberId == userId }
        }

    override suspend fun getActiveTrip(userId: String): Trip? =
        trips.value.values.firstOrNull { it.ownerId == userId || it.memberId == userId }

    override suspend fun save(trip: Trip) {
        savedTrips += trip
        trips.update { it + (trip.id to trip) }
    }
}
