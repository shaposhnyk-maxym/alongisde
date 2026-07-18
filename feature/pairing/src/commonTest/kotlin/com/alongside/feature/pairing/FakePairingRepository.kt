package com.alongside.feature.pairing

import com.alongside.core.domain.pairing.JoinTripResult
import com.alongside.core.domain.pairing.PairingRepository
import com.alongside.core.model.SyncStatus
import com.alongside.core.model.trip.Trip
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

internal fun fakeTrip(
    id: String = "trip-1",
    ownerId: String = "uid-1",
    memberId: String? = null,
    inviteCode: String = "ABCD23",
    startDate: LocalDate = LocalDate(2026, 7, 18),
    endDate: LocalDate = LocalDate(2026, 8, 1),
): Trip =
    Trip(
        id = id,
        ownerId = ownerId,
        memberId = memberId,
        inviteCode = inviteCode,
        startDate = startDate,
        endDate = endDate,
        syncStatus = SyncStatus.PENDING,
        createdAt = Instant.fromEpochMilliseconds(0),
    )

/**
 * Scripted [PairingRepository]: [activeTrip] is pushable from tests to simulate the partner
 * joining remotely; [joinTrip] answers with [nextJoinResult] and, on success, pushes the
 * joined trip the way the real data layer's observation would.
 */
internal class FakePairingRepository : PairingRepository {
    val activeTrip = MutableStateFlow<Trip?>(null)
    val createdTrips = mutableListOf<Trip>()
    val joinCalls = mutableListOf<Pair<String, String>>()
    var nextJoinResult: JoinTripResult = JoinTripResult.InvalidCode

    override suspend fun createTrip(
        ownerId: String,
        startDate: LocalDate,
        endDate: LocalDate,
    ): Trip {
        val trip =
            fakeTrip(
                id = "trip-${createdTrips.size + 1}",
                ownerId = ownerId,
                startDate = startDate,
                endDate = endDate,
            )
        createdTrips += trip
        activeTrip.value = trip
        return trip
    }

    override suspend fun joinTrip(
        code: String,
        userId: String,
    ): JoinTripResult {
        joinCalls += code to userId
        val result = nextJoinResult
        if (result is JoinTripResult.Joined) {
            activeTrip.value = result.trip
        }
        return result
    }

    override fun observeActiveTrip(userId: String): Flow<Trip?> = activeTrip
}
