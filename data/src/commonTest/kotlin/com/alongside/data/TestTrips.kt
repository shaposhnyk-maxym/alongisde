package com.alongside.data

import com.alongside.core.model.SyncStatus
import com.alongside.core.model.trip.Trip
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

internal fun testTrip(
    id: String = "trip-1",
    ownerId: String = "owner-1",
    memberId: String? = null,
    inviteCode: String = "ABCD23",
    syncStatus: SyncStatus = SyncStatus.PENDING,
    createdAt: Instant = Instant.fromEpochMilliseconds(1_752_600_000_000),
    updatedAt: Instant = createdAt,
): Trip =
    Trip(
        id = id,
        ownerId = ownerId,
        memberId = memberId,
        inviteCode = inviteCode,
        startDate = LocalDate(2026, 7, 18),
        endDate = LocalDate(2026, 8, 1),
        syncStatus = syncStatus,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
