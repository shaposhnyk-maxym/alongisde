package com.alongside.feature.diary.presentation

import com.alongside.feature.diary.fakeTrip
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

private val FIXED_TODAY = LocalDate(2026, 7, 20)

class DiaryTimelineStateTest {
    @Test
    fun `the countdown is hidden once the reunion day has arrived`() {
        val trip = fakeTrip(startDate = FIXED_TODAY, endDate = FIXED_TODAY.plus(1, DateTimeUnit.DAY))
        val state = DiaryTimelineState(today = FIXED_TODAY, trip = trip)

        assertEquals(2, state.items.size)
        assertIs<DiaryTimelineItem.Day>(state.items[0])
    }

    @Test
    fun `the countdown is hidden once the reunion day is in the past`() {
        val trip =
            fakeTrip(
                startDate = FIXED_TODAY.plus(-2, DateTimeUnit.DAY),
                endDate = FIXED_TODAY.plus(1, DateTimeUnit.DAY),
            )
        val state = DiaryTimelineState(today = FIXED_TODAY, trip = trip)

        assertEquals(4, state.items.size)
        assertIs<DiaryTimelineItem.Day>(state.items[0])
    }

    @Test
    fun `the countdown shows as the first item while the reunion day is still ahead`() {
        val trip =
            fakeTrip(
                startDate = FIXED_TODAY.plus(5, DateTimeUnit.DAY),
                endDate = FIXED_TODAY.plus(6, DateTimeUnit.DAY),
            )
        val state = DiaryTimelineState(today = FIXED_TODAY, trip = trip)

        val countdown = assertIs<DiaryTimelineItem.Countdown>(state.items.first())
        assertEquals(5, countdown.daysUntilReunion)
    }
}
