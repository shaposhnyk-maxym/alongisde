package com.alongside.core.domain.recap

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

private val AVAILABLE_AT = LocalDate(2026, 7, 20)
private val AVAILABLE_AT_MIDNIGHT = AVAILABLE_AT.atStartOfDayIn(TimeZone.UTC)

class RecapNotificationTimingTest {
    @Test
    fun `exactly one day before midnight counts down a full day`() {
        assertEquals(
            24.hours,
            durationUntilRecapNotification(AVAILABLE_AT, now = AVAILABLE_AT_MIDNIGHT - 24.hours, zone = TimeZone.UTC),
        )
    }

    @Test
    fun `partway through the day before counts down the remaining hours`() {
        assertEquals(
            6.hours,
            durationUntilRecapNotification(AVAILABLE_AT, now = AVAILABLE_AT_MIDNIGHT - 6.hours, zone = TimeZone.UTC),
        )
    }

    @Test
    fun `now already at availableAt's midnight is zero`() {
        assertEquals(
            Duration.ZERO,
            durationUntilRecapNotification(AVAILABLE_AT, now = AVAILABLE_AT_MIDNIGHT, zone = TimeZone.UTC),
        )
    }

    @Test
    fun `availableAt already in the past clamps to zero never negative`() {
        assertEquals(
            Duration.ZERO,
            durationUntilRecapNotification(AVAILABLE_AT, now = AVAILABLE_AT_MIDNIGHT + 24.hours, zone = TimeZone.UTC),
        )
    }
}
