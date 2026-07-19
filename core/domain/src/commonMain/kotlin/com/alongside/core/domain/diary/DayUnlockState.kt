package com.alongside.core.domain.diary

/** Symmetric unlock (CLAUDE.md ADR #5): a day only opens once BOTH sides are READY. */
public enum class DayUnlockState {
    LOCKED,
    UNLOCKED,
}

public fun resolveDayUnlockState(
    own: DiaryDayStatus,
    partner: DiaryDayStatus,
): DayUnlockState =
    if (own == DiaryDayStatus.READY && partner == DiaryDayStatus.READY) {
        DayUnlockState.UNLOCKED
    } else {
        DayUnlockState.LOCKED
    }
