package com.alongside.core.domain.diary

import kotlinx.datetime.LocalDate

/**
 * Fires once, on the trip's last day, the moment both sides are READY (docs/roadmap.md M20.1) -
 * this is the "write step" the roadmap assumes exists for the day-unlock state but that
 * `resolveDayUnlockState` never actually persists.
 */
public fun shouldScheduleRecap(
    date: LocalDate,
    tripEndDate: LocalDate,
    own: DiaryDayStatus,
    partner: DiaryDayStatus,
): Boolean = date == tripEndDate && own == DiaryDayStatus.READY && partner == DiaryDayStatus.READY
