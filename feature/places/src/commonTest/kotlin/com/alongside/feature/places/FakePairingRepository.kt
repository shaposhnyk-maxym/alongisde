package com.alongside.feature.places

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
): Trip =
    Trip(
        id = id,
        ownerId = ownerId,
        memberId = null,
        inviteCode = "ABCD23",
        startDate = LocalDate(2026, 7, 18),
        endDate = LocalDate(2026, 8, 1),
        syncStatus = SyncStatus.PENDING,
        createdAt = Instant.fromEpochMilliseconds(0),
        updatedAt = Instant.fromEpochMilliseconds(0),
    )

/** Scripted [PairingRepository] - only [observeActiveTrip] is exercised by this feature's tests. */
internal class FakePairingRepository(
    initialActiveTrip: Trip? = fakeTrip(),
) : PairingRepository {
    val activeTrip = MutableStateFlow(initialActiveTrip)

    override suspend fun createTrip(
        ownerId: String,
        startDate: LocalDate,
        endDate: LocalDate,
    ): Trip = throw NotImplementedError("not exercised by feature:places")

    override suspend fun joinTrip(
        code: String,
        userId: String,
    ): JoinTripResult = throw NotImplementedError("not exercised by feature:places")

    override fun observeActiveTrip(userId: String): Flow<Trip?> = activeTrip
}
