package com.alongside.core.domain.diary

import com.alongside.core.model.SyncStatus
import com.alongside.core.model.diary.DiaryEntry

public enum class DiaryDayStatus {
    NOT_READY,
    PENDING_SYNC,
    READY,
}

/**
 * No entry yet -> [DiaryDayStatus.NOT_READY]; entry captured but not yet confirmed on the
 * partner's device -> [DiaryDayStatus.PENDING_SYNC] (covers PENDING/SYNCING/FAILED alike -
 * from the unlock rule's perspective, "not there yet" is all that matters); synced -> READY.
 */
public fun diaryDayStatus(entry: DiaryEntry?): DiaryDayStatus =
    when {
        entry == null -> DiaryDayStatus.NOT_READY
        entry.syncStatus == SyncStatus.SYNCED -> DiaryDayStatus.READY
        else -> DiaryDayStatus.PENDING_SYNC
    }
