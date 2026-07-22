package com.alongside.core.domain.diary

/**
 * Why a [DayUnlockState.LOCKED] day is still locked, for the Timeline's waiting-state UI
 * (docs/roadmap.md M12; MISSED added M12.12). [MISSED] wins over every other reason - if either
 * side's day is permanently gone, that's a more relevant answer than "still syncing"/"still
 * capturing" (both of which imply it could still resolve on its own, which MISSED never does).
 * One generic [MISSED] reason regardless of which side missed - the copy doesn't distinguish
 * "you"/"partner", a deliberate simplification agreed with the user. Otherwise, unchanged from
 * before M12.12: partner not having captured anything yet ("they haven't even started") is the
 * only distinction the two-state UI needs; whether *this* side is also still pending doesn't
 * change what the viewer is waiting on.
 */
public enum class DiaryDayLockReason {
    MISSED,
    PARTNER_CAPTURING,
    WAITING_FOR_SYNC,
}

public fun resolveDayLockReason(
    own: DiaryDayStatus,
    partner: DiaryDayStatus,
): DiaryDayLockReason =
    when {
        own == DiaryDayStatus.MISSED || partner == DiaryDayStatus.MISSED -> DiaryDayLockReason.MISSED
        partner == DiaryDayStatus.NOT_READY -> DiaryDayLockReason.PARTNER_CAPTURING
        else -> DiaryDayLockReason.WAITING_FOR_SYNC
    }
