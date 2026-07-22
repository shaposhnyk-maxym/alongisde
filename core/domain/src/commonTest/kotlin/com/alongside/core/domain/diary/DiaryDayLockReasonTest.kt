package com.alongside.core.domain.diary

import kotlin.test.Test
import kotlin.test.assertEquals

class DiaryDayLockReasonTest {
    @Test
    fun `partner not ready is partner capturing`() {
        assertEquals(
            DiaryDayLockReason.PARTNER_CAPTURING,
            resolveDayLockReason(own = DiaryDayStatus.NOT_READY, partner = DiaryDayStatus.NOT_READY),
        )
    }

    @Test
    fun `partner pending sync is waiting for sync`() {
        assertEquals(
            DiaryDayLockReason.WAITING_FOR_SYNC,
            resolveDayLockReason(own = DiaryDayStatus.NOT_READY, partner = DiaryDayStatus.PENDING_SYNC),
        )
    }

    @Test
    fun `partner ready is waiting for sync`() {
        assertEquals(
            DiaryDayLockReason.WAITING_FOR_SYNC,
            resolveDayLockReason(own = DiaryDayStatus.NOT_READY, partner = DiaryDayStatus.READY),
        )
    }

    @Test
    fun `own missed is missed regardless of partner`() {
        assertEquals(
            DiaryDayLockReason.MISSED,
            resolveDayLockReason(own = DiaryDayStatus.MISSED, partner = DiaryDayStatus.READY),
        )
    }

    @Test
    fun `partner missed is missed regardless of own`() {
        assertEquals(
            DiaryDayLockReason.MISSED,
            resolveDayLockReason(own = DiaryDayStatus.READY, partner = DiaryDayStatus.MISSED),
        )
    }

    @Test
    fun `neither missed still falls back to partner-driven reasons`() {
        assertEquals(
            DiaryDayLockReason.PARTNER_CAPTURING,
            resolveDayLockReason(own = DiaryDayStatus.READY, partner = DiaryDayStatus.NOT_READY),
        )
    }
}
