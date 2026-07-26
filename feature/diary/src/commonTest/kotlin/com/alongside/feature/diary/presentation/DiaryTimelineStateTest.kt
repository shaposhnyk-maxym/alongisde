package com.alongside.feature.diary.presentation

import com.alongside.feature.diary.fakeTrip
import com.alongside.feature.diary.testPreTripPhoto
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

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

    @Test
    fun `the countdown carries its own pre-trip photos`() {
        val trip =
            fakeTrip(
                startDate = FIXED_TODAY.plus(5, DateTimeUnit.DAY),
                endDate = FIXED_TODAY.plus(6, DateTimeUnit.DAY),
            )
        val ownPhotos = listOf(testPreTripPhoto("own-1"), testPreTripPhoto("own-2"))
        val partnerPhotos = listOf(testPreTripPhoto("partner-1", userId = "partner-1"))
        val state =
            DiaryTimelineState(
                today = FIXED_TODAY,
                trip = trip,
                ownPreTripPhotos = ownPhotos,
                partnerPreTripPhotos = partnerPhotos,
            )

        val countdown = assertIs<DiaryTimelineItem.Countdown>(state.items.first())
        assertEquals(ownPhotos, countdown.ownPhotos)
        assertEquals(partnerPhotos, countdown.partnerPhotos)
    }

    @Test
    fun `the countdown with pre-trip photos still disappears once the reunion day has arrived`() {
        val trip = fakeTrip(startDate = FIXED_TODAY, endDate = FIXED_TODAY.plus(1, DateTimeUnit.DAY))
        val state =
            DiaryTimelineState(
                today = FIXED_TODAY,
                trip = trip,
                ownPreTripPhotos = List(3) { testPreTripPhoto("own-$it") },
                partnerPreTripPhotos = List(3) { testPreTripPhoto("partner-$it", userId = "partner-1") },
            )

        assertTrue(state.items.none { it is DiaryTimelineItem.Countdown })
    }

    @Test
    fun `the countdown never survives the transition from one day left to zero regardless of pre-trip photo count`() {
        for (photoCount in 0..3) {
            val ownPhotos = List(photoCount) { testPreTripPhoto("own-$it") }

            val dayBefore =
                DiaryTimelineState(
                    today = FIXED_TODAY,
                    trip = fakeTrip(startDate = FIXED_TODAY.plus(1, DateTimeUnit.DAY), endDate = FIXED_TODAY.plus(2, DateTimeUnit.DAY)),
                    ownPreTripPhotos = ownPhotos,
                )
            val reunionDay =
                DiaryTimelineState(
                    today = FIXED_TODAY,
                    trip = fakeTrip(startDate = FIXED_TODAY, endDate = FIXED_TODAY.plus(1, DateTimeUnit.DAY)),
                    ownPreTripPhotos = ownPhotos,
                )

            assertTrue(
                dayBefore.items.any { it is DiaryTimelineItem.Countdown },
                "expected a Countdown item with $photoCount photo(s) one day before reunion",
            )
            assertTrue(
                reunionDay.items.none { it is DiaryTimelineItem.Countdown },
                "expected no Countdown item with $photoCount photo(s) on the reunion day",
            )
        }
    }
}
