package com.alongside.core.domain.diary

import com.alongside.core.model.diary.DiaryEntry

/**
 * The condition M17's Cloud Function trigger fires on - both sides' [DiaryEntry] for the same
 * date are confirmed synced. Logic only: no FCM call happens here, that's M17.
 */
public fun shouldTriggerPartnerReadyPush(
    own: DiaryEntry?,
    partner: DiaryEntry?,
): Boolean = diaryDayStatus(own) == DiaryDayStatus.READY && diaryDayStatus(partner) == DiaryDayStatus.READY
