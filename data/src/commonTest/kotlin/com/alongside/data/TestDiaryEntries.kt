package com.alongside.data

import com.alongside.core.model.SyncStatus
import com.alongside.core.model.diary.DiaryEntry
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

internal fun testDiaryEntry(
    id: String = "entry-1",
    tripId: String = "trip-1",
    userId: String = "owner-1",
    date: LocalDate = LocalDate(2026, 7, 18),
    syncStatus: SyncStatus = SyncStatus.PENDING,
    createdAt: Instant = Instant.fromEpochMilliseconds(1_752_600_000_000),
    updatedAt: Instant = createdAt,
    closedAt: Instant? = null,
): DiaryEntry =
    DiaryEntry(
        id = id,
        tripId = tripId,
        userId = userId,
        date = date,
        syncStatus = syncStatus,
        createdAt = createdAt,
        updatedAt = updatedAt,
        closedAt = closedAt,
    )
