package com.alongside.feature.pairing.presentation

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class DatePickerConversionsTest {
    @Test
    fun `a LocalDate converts to the UTC start-of-day epoch millis Material3 pickers expect`() {
        assertEquals(1_784_332_800_000L, LocalDate(2026, 7, 18).toUtcEpochMillis())
    }

    @Test
    fun `epoch millis convert back to the same LocalDate`() {
        assertEquals(LocalDate(2026, 7, 18), 1_784_332_800_000L.toLocalDateFromUtcEpochMillis())
    }

    @Test
    fun `round-trips across a year boundary`() {
        val date = LocalDate(2026, 12, 31)
        assertEquals(date, date.toUtcEpochMillis().toLocalDateFromUtcEpochMillis())
    }

    @Test
    fun `round-trips across a leap-day boundary`() {
        val date = LocalDate(2028, 2, 29)
        assertEquals(date, date.toUtcEpochMillis().toLocalDateFromUtcEpochMillis())
    }
}
