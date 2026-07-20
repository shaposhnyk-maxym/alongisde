package com.alongside.core.domain.trip

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class ReunionCountdownTest {
    @Test
    fun `days until a future meeting date counts up correctly`() {
        assertEquals(
            14,
            daysUntilReunion(today = LocalDate(2026, 7, 18), meetingDate = LocalDate(2026, 8, 1)),
        )
    }

    @Test
    fun `one day away counts as one`() {
        assertEquals(
            1,
            daysUntilReunion(today = LocalDate(2026, 7, 31), meetingDate = LocalDate(2026, 8, 1)),
        )
    }

    @Test
    fun `meeting today counts as zero`() {
        assertEquals(
            0,
            daysUntilReunion(today = LocalDate(2026, 8, 1), meetingDate = LocalDate(2026, 8, 1)),
        )
    }

    @Test
    fun `a meeting date already in the past clamps to zero never negative`() {
        assertEquals(
            0,
            daysUntilReunion(today = LocalDate(2026, 8, 5), meetingDate = LocalDate(2026, 8, 1)),
        )
    }
}
