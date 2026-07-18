package com.alongside.core.domain.pairing

import com.alongside.core.model.SyncStatus
import com.alongside.core.model.trip.Trip
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

internal fun pairingTestTrip(
    id: String = "trip-1",
    ownerId: String = "owner-1",
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
        updatedAt = Instant.fromEpochMilliseconds(0),
    )
