package com.alongside.core.domain.diary

import kotlin.test.Test
import kotlin.test.assertEquals

class DiaryDayLockReasonTest {
    @Test
    fun `partner not ready is partner capturing`() {
        assertEquals(DiaryDayLockReason.PARTNER_CAPTURING, resolveDayLockReason(DiaryDayStatus.NOT_READY))
    }

    @Test
    fun `partner pending sync is waiting for sync`() {
        assertEquals(DiaryDayLockReason.WAITING_FOR_SYNC, resolveDayLockReason(DiaryDayStatus.PENDING_SYNC))
    }

    @Test
    fun `partner ready is waiting for sync`() {
        assertEquals(DiaryDayLockReason.WAITING_FOR_SYNC, resolveDayLockReason(DiaryDayStatus.READY))
    }
}
