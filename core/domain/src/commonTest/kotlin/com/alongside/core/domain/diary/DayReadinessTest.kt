package com.alongside.core.domain.diary

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DayReadinessTest {
    @Test
    fun `neither ready returns false`() {
        assertFalse(
            isDayUnlocked(own = DiaryDayStatus.NOT_READY, partner = DiaryDayStatus.NOT_READY),
        )
    }

    @Test
    fun `only own ready returns false`() {
        assertFalse(
            isDayUnlocked(own = DiaryDayStatus.READY, partner = DiaryDayStatus.NOT_READY),
        )
    }

    @Test
    fun `only partner ready returns false`() {
        assertFalse(
            isDayUnlocked(own = DiaryDayStatus.NOT_READY, partner = DiaryDayStatus.READY),
        )
    }

    @Test
    fun `both ready returns true`() {
        assertTrue(
            isDayUnlocked(own = DiaryDayStatus.READY, partner = DiaryDayStatus.READY),
        )
    }
}
