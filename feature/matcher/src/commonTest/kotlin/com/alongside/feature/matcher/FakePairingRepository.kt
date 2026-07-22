package com.alongside.feature.matcher

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
    ownerId: String = "owner-1",
    memberId: String? = "member-1",
): Trip =
    Trip(
        id = id,
        ownerId = ownerId,
        memberId = memberId,
        inviteCode = "ABCD23",
        startDate = LocalDate(2026, 7, 18),
        endDate = LocalDate(2026, 8, 1),
        syncStatus = SyncStatus.SYNCED,
        createdAt = Instant.fromEpochMilliseconds(0),
        updatedAt = Instant.fromEpochMilliseconds(0),
    )

/** Only [observeActiveTrip]/[getActiveTrip] are exercised by Matcher - create/join belong to feature:pairing. */
internal class FakePairingRepository : PairingRepository {
    val activeTrip = MutableStateFlow<Trip?>(null)

    override suspend fun createTrip(
        ownerId: String,
        startDate: LocalDate,
        endDate: LocalDate,
    ): Trip = error("not used by Matcher")

    override suspend fun joinTrip(
        code: String,
        userId: String,
    ): JoinTripResult = error("not used by Matcher")

    override fun observeActiveTrip(userId: String): Flow<Trip?> = activeTrip

    override suspend fun getActiveTrip(userId: String): Trip? = activeTrip.value
}
