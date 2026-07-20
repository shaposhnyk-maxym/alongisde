package com.alongside.core.domain.diary

/**
 * Why a [DayUnlockState.LOCKED] day is still locked, for the Timeline's waiting-state UI
 * (docs/roadmap.md M12). Derived only from [partner]'s persisted [DiaryDayStatus] - partner not
 * having captured anything yet ("they haven't even started") is the only distinction the current
 * two-state UI needs; whether *this* side is also still pending doesn't change what the viewer is
 * waiting on.
 */
public enum class DiaryDayLockReason {
    PARTNER_CAPTURING,
    WAITING_FOR_SYNC,
}

public fun resolveDayLockReason(partner: DiaryDayStatus): DiaryDayLockReason =
    if (partner == DiaryDayStatus.NOT_READY) {
        DiaryDayLockReason.PARTNER_CAPTURING
    } else {
        DiaryDayLockReason.WAITING_FOR_SYNC
    }
