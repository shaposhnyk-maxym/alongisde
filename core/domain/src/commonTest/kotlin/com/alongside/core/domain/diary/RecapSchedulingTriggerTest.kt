package com.alongside.core.domain.diary

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private val TRIP_END_DATE = LocalDate(2026, 7, 19)

class RecapSchedulingTriggerTest {
    @Test
    fun `last day with both sides ready schedules the recap`() {
        assertTrue(
            shouldScheduleRecap(
                date = TRIP_END_DATE,
                tripEndDate = TRIP_END_DATE,
                own = DiaryDayStatus.READY,
                partner = DiaryDayStatus.READY,
            ),
        )
    }

    @Test
    fun `a non-final day with both sides ready does not schedule the recap`() {
        assertFalse(
            shouldScheduleRecap(
                date = TRIP_END_DATE.minus(DatePeriod(days = 1)),
                tripEndDate = TRIP_END_DATE,
                own = DiaryDayStatus.READY,
                partner = DiaryDayStatus.READY,
            ),
        )
    }

    @Test
    fun `last day with only own side ready does not schedule the recap`() {
        assertFalse(
            shouldScheduleRecap(
                date = TRIP_END_DATE,
                tripEndDate = TRIP_END_DATE,
                own = DiaryDayStatus.READY,
                partner = DiaryDayStatus.NOT_READY,
            ),
        )
    }

    @Test
    fun `last day with only partner side ready does not schedule the recap`() {
        assertFalse(
            shouldScheduleRecap(
                date = TRIP_END_DATE,
                tripEndDate = TRIP_END_DATE,
                own = DiaryDayStatus.NOT_READY,
                partner = DiaryDayStatus.READY,
            ),
        )
    }
}
