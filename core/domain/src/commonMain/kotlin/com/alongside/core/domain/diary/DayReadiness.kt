package com.alongside.core.domain.diary

public fun isDayUnlocked(
    own: DiaryDayStatus,
    partner: DiaryDayStatus,
): Boolean = own == DiaryDayStatus.READY && partner == DiaryDayStatus.READY
