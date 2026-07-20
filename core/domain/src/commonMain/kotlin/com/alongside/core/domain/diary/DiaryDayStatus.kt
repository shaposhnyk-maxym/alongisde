package com.alongside.core.domain.diary

import com.alongside.core.model.SyncStatus
import com.alongside.core.model.diary.DiaryEntry
import kotlinx.datetime.LocalDate

public enum class DiaryDayStatus {
    NOT_READY,
    PENDING_SYNC,
    OPEN,
    READY,
}

/**
 * No entry yet -> [DiaryDayStatus.NOT_READY]; entry captured but not yet confirmed on the
 * partner's device -> [DiaryDayStatus.PENDING_SYNC] (covers PENDING/SYNCING/FAILED alike -
 * from the unlock rule's perspective, "not there yet" is all that matters); synced but neither
 * explicitly closed nor past its own date yet -> [DiaryDayStatus.OPEN] (docs/roadmap.md M12.6 -
 * the whole point is that a day no longer counts as done the moment it merely syncs); explicitly
 * closed, or its date has already passed (no separate timer needed - capture only ever targets
 * `today`, so a bygone day can never receive new episodes anyway) -> READY.
 */
public fun diaryDayStatus(
    entry: DiaryEntry?,
    today: LocalDate,
): DiaryDayStatus =
    when {
        entry == null -> DiaryDayStatus.NOT_READY
        entry.syncStatus != SyncStatus.SYNCED -> DiaryDayStatus.PENDING_SYNC
        entry.closedAt != null || entry.date < today -> DiaryDayStatus.READY
        else -> DiaryDayStatus.OPEN
    }
