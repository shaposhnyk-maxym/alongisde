package com.alongside.app.home

import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.orbitmvi.orbit.test.test
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

private val TRIP_END_DATE = LocalDate(2026, 7, 19)
private val RECAP_AVAILABLE_AT = LocalDate(2026, 7, 20)

private class FixedClock(
    private val today: LocalDate,
) : Clock {
    // epoch-day arithmetic is the simplest deterministic "this LocalDate as an Instant" - no
    // direct helper for it in the kotlinx-datetime API surface already used elsewhere here.
    override fun now(): Instant = Instant.fromEpochMilliseconds(today.toEpochDays() * 86_400_000L)
}

class HomeContainerTest {
    private val authSessionCache = FakeAuthSessionCache(testAuthSession("uid-1"))
    private val pairingRepository = FakePairingRepository()
    private val recapRepository = FakeRecapRepository()

    private fun containerUnderTest(today: LocalDate) =
        HomeContainer(
            authSessionCache = authSessionCache,
            pairingRepository = pairingRepository,
            recapRepository = recapRepository,
            clock = FixedClock(today),
        )

    @Test
    fun `recap is unavailable before its availableAt date`() =
        runTest {
            val trip = fakeTrip(id = "trip-1", endDate = TRIP_END_DATE)
            pairingRepository.activeTrip.value = trip
            recapRepository.ensureScheduled(trip.id, RECAP_AVAILABLE_AT)

            // Seeded true so the settle-to-false reduce produces a detectable state change -
            // it lands on the same false value the real default already holds, and a
            // StateFlow conflates equal values, so relying on the default would never emit.
            containerUnderTest(today = TRIP_END_DATE).test(this, initialState = HomeState(isRecapAvailable = true)) {
                runOnCreate()
                val state = awaitState()
                assertFalse(state.isRecapAvailable)
                cancelAndIgnoreRemainingItems()
            }
        }

    @Test
    fun `recap is available once today reaches its availableAt date`() =
        runTest {
            val trip = fakeTrip(id = "trip-1", endDate = TRIP_END_DATE)
            pairingRepository.activeTrip.value = trip
            recapRepository.ensureScheduled(trip.id, RECAP_AVAILABLE_AT)

            containerUnderTest(today = RECAP_AVAILABLE_AT).test(this) {
                runOnCreate()
                val state = awaitState()
                assertTrue(state.isRecapAvailable)
                cancelAndIgnoreRemainingItems()
            }
        }

    @Test
    fun `no active trip leaves the recap unavailable`() =
        runTest {
            containerUnderTest(today = RECAP_AVAILABLE_AT).test(this, initialState = HomeState(isRecapAvailable = true)) {
                runOnCreate()
                val state = awaitState()
                assertFalse(state.isRecapAvailable)
                cancelAndIgnoreRemainingItems()
            }
        }

    @Test
    fun `an active trip with no recap row yet leaves the recap unavailable without crashing`() =
        runTest {
            val trip = fakeTrip(id = "trip-1", endDate = TRIP_END_DATE)
            pairingRepository.activeTrip.value = trip

            containerUnderTest(today = RECAP_AVAILABLE_AT).test(this, initialState = HomeState(isRecapAvailable = true)) {
                runOnCreate()
                val state = awaitState()
                assertFalse(state.isRecapAvailable)
                cancelAndIgnoreRemainingItems()
            }
        }
}
