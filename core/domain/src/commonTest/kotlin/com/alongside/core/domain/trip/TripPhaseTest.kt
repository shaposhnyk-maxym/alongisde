package com.alongside.core.domain.trip

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class TripPhaseTest {
    private val trip = tripManagementTestTrip(startDate = LocalDate(2026, 7, 18), endDate = LocalDate(2026, 8, 1))

    @Test
    fun `the day before the trip starts is pre-trip`() {
        assertEquals(TripPhase.PRE_TRIP, tripPhase(trip, today = LocalDate(2026, 7, 17)))
    }

    @Test
    fun `the start date itself is active`() {
        assertEquals(TripPhase.ACTIVE, tripPhase(trip, today = LocalDate(2026, 7, 18)))
    }

    @Test
    fun `a day in the middle of the trip is active`() {
        assertEquals(TripPhase.ACTIVE, tripPhase(trip, today = LocalDate(2026, 7, 25)))
    }

    @Test
    fun `the end date itself is still active`() {
        assertEquals(TripPhase.ACTIVE, tripPhase(trip, today = LocalDate(2026, 8, 1)))
    }

    @Test
    fun `the day after the trip ends is post-trip`() {
        assertEquals(TripPhase.POST_TRIP, tripPhase(trip, today = LocalDate(2026, 8, 2)))
    }
}
