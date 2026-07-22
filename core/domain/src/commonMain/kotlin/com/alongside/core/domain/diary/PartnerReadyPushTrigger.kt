package com.alongside.core.domain.diary

import com.alongside.core.model.diary.DiaryEntry
import kotlinx.datetime.LocalDate

/**
 * The condition M17's Cloud Function trigger fires on - both sides' [DiaryEntry] for the same
 * date are confirmed synced AND closed (docs/roadmap.md M12.6 - syncing alone is no longer
 * "ready"), or the day has passed with at least one episode on each side (docs/roadmap.md
 * M12.12 - a day that passed with zero episodes is MISSED, never READY, and must not trigger
 * this push either). Logic only: no FCM call happens here, that's M17.
 */
public fun shouldTriggerPartnerReadyPush(
    own: DiaryEntry?,
    partner: DiaryEntry?,
    today: LocalDate,
    ownHasEpisodes: Boolean = false,
    partnerHasEpisodes: Boolean = false,
): Boolean {
    val ownReady = diaryDayStatus(own, own?.date ?: today, today, ownHasEpisodes) == DiaryDayStatus.READY
    val partnerReady =
        diaryDayStatus(partner, partner?.date ?: today, today, partnerHasEpisodes) == DiaryDayStatus.READY
    return ownReady && partnerReady
}
