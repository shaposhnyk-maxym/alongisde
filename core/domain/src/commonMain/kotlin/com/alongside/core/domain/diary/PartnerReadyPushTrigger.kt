package com.alongside.core.domain.diary

import com.alongside.core.model.diary.DiaryEntry
import kotlinx.datetime.LocalDate

/**
 * The condition M17's Cloud Function trigger fires on - both sides' [DiaryEntry] for the same
 * date are confirmed synced AND closed (docs/roadmap.md M12.6 - syncing alone is no longer
 * "ready"). Logic only: no FCM call happens here, that's M17.
 */
public fun shouldTriggerPartnerReadyPush(
    own: DiaryEntry?,
    partner: DiaryEntry?,
    today: LocalDate,
): Boolean {
    val ownReady = diaryDayStatus(own, today) == DiaryDayStatus.READY
    val partnerReady = diaryDayStatus(partner, today) == DiaryDayStatus.READY
    return ownReady && partnerReady
}
