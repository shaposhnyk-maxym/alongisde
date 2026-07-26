package com.alongside.feature.recap

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
    memberId: String? = "partner-1",
    inviteCode: String = "ABCD23",
    startDate: LocalDate = LocalDate(2026, 7, 18),
    endDate: LocalDate = LocalDate(2026, 7, 20),
): Trip =
    Trip(
        id = id,
        ownerId = ownerId,
        memberId = memberId,
        inviteCode = inviteCode,
        startDate = startDate,
        endDate = endDate,
        syncStatus = SyncStatus.SYNCED,
        createdAt = Instant.fromEpochMilliseconds(0),
        updatedAt = Instant.fromEpochMilliseconds(0),
    )

/** Only [getActiveTrip] is exercised by Recap - create/join belong to feature:pairing. */
internal class FakePairingRepository : PairingRepository {
    val activeTrip = MutableStateFlow<Trip?>(null)

    override suspend fun createTrip(
        ownerId: String,
        startDate: LocalDate,
        endDate: LocalDate,
    ): Trip = error("not used by Recap")

    override suspend fun joinTrip(
        code: String,
        userId: String,
    ): JoinTripResult = error("not used by Recap")

    override fun observeActiveTrip(userId: String): Flow<Trip?> = activeTrip

    override suspend fun getActiveTrip(userId: String): Trip? = activeTrip.value
}
