package com.alongside.core.domain.diary

import com.alongside.core.model.SyncStatus
import com.alongside.core.model.diary.DiaryEntry
import kotlinx.datetime.LocalDate

public enum class DiaryDayStatus {
    NOT_READY,
    PENDING_SYNC,
    OPEN,
    READY,
    MISSED,
}

/**
 * No entry yet, day still today/future -> [DiaryDayStatus.NOT_READY]; no entry and the day has
 * already passed -> [DiaryDayStatus.MISSED] (docs/roadmap.md M12.12 - nothing was ever captured,
 * and the day it belonged to is gone). Entry captured but not yet confirmed on the partner's
 * device -> [DiaryDayStatus.PENDING_SYNC] (covers PENDING/SYNCING/FAILED alike - from the unlock
 * rule's perspective, "not there yet" is all that matters). Synced but neither explicitly closed
 * nor past its own date yet -> [DiaryDayStatus.OPEN] (docs/roadmap.md M12.6 - the whole point is
 * that a day no longer counts as done the moment it merely syncs). Explicitly closed -> READY
 * regardless of [hasEpisodes] (an explicit close always wins). Otherwise, once the entry's own
 * date has passed: READY if [hasEpisodes], MISSED if not (docs/roadmap.md M12.12 - an entry with
 * zero episodes, e.g. every photo cluster failed to process, must not silently count as "done").
 */
public fun diaryDayStatus(
    entry: DiaryEntry?,
    date: LocalDate,
    today: LocalDate,
    hasEpisodes: Boolean,
): DiaryDayStatus =
    when {
        entry == null -> if (date < today) DiaryDayStatus.MISSED else DiaryDayStatus.NOT_READY
        entry.syncStatus != SyncStatus.SYNCED -> DiaryDayStatus.PENDING_SYNC
        entry.closedAt != null -> DiaryDayStatus.READY
        entry.date < today -> if (hasEpisodes) DiaryDayStatus.READY else DiaryDayStatus.MISSED
        else -> DiaryDayStatus.OPEN
    }
