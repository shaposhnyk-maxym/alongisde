package com.alongside.core.domain.recap

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class RecapAvailabilityTest {
    @Test
    fun `recap becomes available the day after the trip ends`() {
        assertEquals(LocalDate(2026, 7, 20), recapAvailableAt(tripEndDate = LocalDate(2026, 7, 19)))
    }
}
