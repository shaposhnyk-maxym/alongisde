package com.alongside.core.domain.diary

/** Symmetric unlock (CLAUDE.md ADR #5): a day only opens once BOTH sides are READY. */
public enum class DayUnlockState {
    LOCKED,
    UNLOCKED,
}

public fun resolveDayUnlockState(
    own: DiaryDayStatus,
    partner: DiaryDayStatus,
    isFinalDay: Boolean = false,
    recapScheduled: Boolean = true,
): DayUnlockState {
    val bothReady = own == DiaryDayStatus.READY && partner == DiaryDayStatus.READY
    val recapGateClear = !isFinalDay || recapScheduled
    return if (bothReady && recapGateClear) DayUnlockState.UNLOCKED else DayUnlockState.LOCKED
}
