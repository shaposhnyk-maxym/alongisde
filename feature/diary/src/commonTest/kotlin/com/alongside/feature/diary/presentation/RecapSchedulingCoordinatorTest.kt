package com.alongside.feature.diary.presentation

import com.alongside.core.domain.diary.DiaryDayStatus
import com.alongside.feature.diary.FakeRecapRepository
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val TRIP_END_DATE = LocalDate(2026, 7, 19)

class RecapSchedulingCoordinatorTest {
    @Test
    fun `last day with both sides ready schedules the recap exactly once`() =
        runTest {
            val recapRepository = FakeRecapRepository()
            val coordinator = RecapSchedulingCoordinator(recapRepository)

            coordinator.ensureScheduledIfReady(
                tripId = "trip-1",
                date = TRIP_END_DATE,
                tripEndDate = TRIP_END_DATE,
                own = DiaryDayStatus.READY,
                partner = DiaryDayStatus.READY,
            )

            assertEquals(
                listOf("trip-1" to LocalDate(2026, 7, 20)),
                recapRepository.ensureScheduledCalls,
            )
        }

    @Test
    fun `a non-final day with both sides ready schedules nothing`() =
        runTest {
            val recapRepository = FakeRecapRepository()
            val coordinator = RecapSchedulingCoordinator(recapRepository)

            coordinator.ensureScheduledIfReady(
                tripId = "trip-1",
                date = LocalDate(2026, 7, 18),
                tripEndDate = TRIP_END_DATE,
                own = DiaryDayStatus.READY,
                partner = DiaryDayStatus.READY,
            )

            assertTrue(recapRepository.ensureScheduledCalls.isEmpty())
        }

    @Test
    fun `last day with one side not ready schedules nothing`() =
        runTest {
            val recapRepository = FakeRecapRepository()
            val coordinator = RecapSchedulingCoordinator(recapRepository)

            coordinator.ensureScheduledIfReady(
                tripId = "trip-1",
                date = TRIP_END_DATE,
                tripEndDate = TRIP_END_DATE,
                own = DiaryDayStatus.READY,
                partner = DiaryDayStatus.NOT_READY,
            )

            assertTrue(recapRepository.ensureScheduledCalls.isEmpty())
        }
}
