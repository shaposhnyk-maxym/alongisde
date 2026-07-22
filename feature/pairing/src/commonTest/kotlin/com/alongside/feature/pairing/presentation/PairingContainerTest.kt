package com.alongside.feature.pairing.presentation

import com.alongside.core.domain.pairing.JoinTripResult
import com.alongside.feature.pairing.FakeAuthSessionCache
import com.alongside.feature.pairing.FakePairingRepository
import com.alongside.feature.pairing.fakeTrip
import com.alongside.feature.pairing.testAuthSession
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import org.orbitmvi.orbit.test.test
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Instant

private val FIXED_NOW = Instant.fromEpochMilliseconds(1_752_800_000_000)

private object FixedClock : Clock {
    override fun now(): Instant = FIXED_NOW
}

class PairingContainerTest {
    private val repository = FakePairingRepository()

    private fun containerUnderTest(uid: String = "uid-1") =
        PairingContainer(
            pairingRepository = repository,
            authSessionCache = FakeAuthSessionCache(testAuthSession(uid)),
            clock = FixedClock,
        )

    @Test
    fun `initial state is the choice step`() =
        runTest {
            containerUnderTest().test(this) {
                // autoCheckInitialState (orbit-test default) already asserted PairingState() on entry.
            }
            assertEquals(PairingStep.CHOICE, PairingState().step)
        }

    @Test
    fun `an existing unpaired own trip is restored into state on start`() =
        runTest {
            val ownTrip = fakeTrip(ownerId = "uid-1")
            repository.activeTrip.value = ownTrip

            containerUnderTest().test(this) {
                runOnCreate()
                expectState { copy(isCheckingTrip = false, ownTrip = ownTrip) }
                cancelAndIgnoreRemainingItems()
            }
        }

    @Test
    fun `an already-paired trip fires Paired on start`() =
        runTest {
            repository.activeTrip.value = fakeTrip(ownerId = "uid-1", memberId = "partner")

            containerUnderTest().test(this) {
                runOnCreate()
                expectSideEffect(PairingSideEffect.Paired)
                cancelAndIgnoreRemainingItems()
            }
        }

    @Test
    fun `no existing trip flips isCheckingTrip to false without a side effect`() =
        runTest {
            containerUnderTest().test(this) {
                runOnCreate()
                expectState { copy(isCheckingTrip = false) }
                cancelAndIgnoreRemainingItems()
            }
        }

    @Test
    fun `no signed-in user leaves isCheckingTrip false without observing anything`() =
        runTest {
            val container =
                PairingContainer(
                    pairingRepository = repository,
                    authSessionCache = FakeAuthSessionCache(session = null),
                    clock = FixedClock,
                )

            container.test(this) {
                runOnCreate()
                expectState { copy(isCheckingTrip = false) }
                cancelAndIgnoreRemainingItems()
            }
        }

    @Test
    fun `picking trip dates opens the date step pre-filled with the default range`() =
        runTest {
            containerUnderTest().test(this) {
                containerHost.onIntent(PairingIntent.PickTripDates)
                val expectedStart = FixedClock.todayIn(TimeZone.currentSystemDefault())
                expectState {
                    copy(
                        isPickingDates = true,
                        tripStartDate = expectedStart,
                        tripEndDate = expectedStart.plus(14, DateTimeUnit.DAY),
                    )
                }
            }
        }

    @Test
    fun `changing the picked dates updates state`() =
        runTest {
            containerUnderTest().test(this) {
                containerHost.onIntent(PairingIntent.PickTripDates)
                val expectedStart = FixedClock.todayIn(TimeZone.currentSystemDefault())
                expectState {
                    copy(
                        isPickingDates = true,
                        tripStartDate = expectedStart,
                        tripEndDate = expectedStart.plus(14, DateTimeUnit.DAY),
                    )
                }

                val customStart = LocalDate(2026, 9, 1)
                val customEnd = LocalDate(2026, 9, 5)
                containerHost.onIntent(PairingIntent.TripDatesChanged(customStart, customEnd))
                expectState { copy(tripStartDate = customStart, tripEndDate = customEnd) }
            }
        }

    @Test
    fun `confirming picked dates creates the trip with exactly those dates`() =
        runTest {
            containerUnderTest().test(this) {
                runOnCreate()
                expectState { copy(isCheckingTrip = false) }
                containerHost.onIntent(PairingIntent.PickTripDates)
                val expectedStart = FixedClock.todayIn(TimeZone.currentSystemDefault())
                expectState {
                    copy(
                        isPickingDates = true,
                        tripStartDate = expectedStart,
                        tripEndDate = expectedStart.plus(14, DateTimeUnit.DAY),
                    )
                }

                val customStart = LocalDate(2026, 9, 1)
                val customEnd = LocalDate(2026, 9, 5)
                containerHost.onIntent(PairingIntent.TripDatesChanged(customStart, customEnd))
                expectState { copy(tripStartDate = customStart, tripEndDate = customEnd) }

                containerHost.onIntent(PairingIntent.ConfirmTripDates)
                expectState { copy(isCreating = true) }
                val created = repository.createdTrips.single()
                expectState { copy(isCreating = false, isPickingDates = false, ownTrip = created) }
                cancelAndIgnoreRemainingItems()
            }
            val created = repository.createdTrips.single()
            assertEquals(PairingStep.CREATE_SHOW_CODE, PairingState(ownTrip = created).step)
            assertEquals("uid-1", created.ownerId)
            assertEquals(LocalDate(2026, 9, 1), created.startDate)
            assertEquals(LocalDate(2026, 9, 5), created.endDate)
        }

    @Test
    fun `confirming with an end date before the start date does not create a trip`() =
        runTest {
            containerUnderTest().test(this) {
                containerHost.onIntent(PairingIntent.PickTripDates)
                val expectedStart = FixedClock.todayIn(TimeZone.currentSystemDefault())
                expectState {
                    copy(
                        isPickingDates = true,
                        tripStartDate = expectedStart,
                        tripEndDate = expectedStart.plus(14, DateTimeUnit.DAY),
                    )
                }

                val invalidStart = LocalDate(2026, 9, 5)
                val invalidEnd = LocalDate(2026, 9, 1)
                containerHost.onIntent(PairingIntent.TripDatesChanged(invalidStart, invalidEnd))
                expectState { copy(tripStartDate = invalidStart, tripEndDate = invalidEnd) }

                containerHost.onIntent(PairingIntent.ConfirmTripDates)
            }
            assertEquals(emptyList(), repository.createdTrips)
        }

    @Test
    fun `back from the date step returns to choice and clears the picked dates`() =
        runTest {
            containerUnderTest().test(this) {
                containerHost.onIntent(PairingIntent.PickTripDates)
                val expectedStart = FixedClock.todayIn(TimeZone.currentSystemDefault())
                expectState {
                    copy(
                        isPickingDates = true,
                        tripStartDate = expectedStart,
                        tripEndDate = expectedStart.plus(14, DateTimeUnit.DAY),
                    )
                }

                containerHost.onIntent(PairingIntent.BackToChoice)
                expectState { copy(isPickingDates = false, tripStartDate = null, tripEndDate = null) }
            }
        }

    @Test
    fun `partner joining while waiting fires Paired`() =
        runTest {
            containerUnderTest().test(this) {
                runOnCreate()
                expectState { copy(isCheckingTrip = false) }
                containerHost.onIntent(PairingIntent.PickTripDates)
                val expectedStart = FixedClock.todayIn(TimeZone.currentSystemDefault())
                expectState {
                    copy(
                        isPickingDates = true,
                        tripStartDate = expectedStart,
                        tripEndDate = expectedStart.plus(14, DateTimeUnit.DAY),
                    )
                }
                containerHost.onIntent(PairingIntent.ConfirmTripDates)
                expectState { copy(isCreating = true) }
                val created = repository.createdTrips.single()
                expectState { copy(isCreating = false, isPickingDates = false, ownTrip = created) }

                repository.activeTrip.value = created.copy(memberId = "partner")
                expectSideEffect(PairingSideEffect.Paired)
                cancelAndIgnoreRemainingItems()
            }
        }

    @Test
    fun `choosing join shows the code entry step and back returns to choice`() =
        runTest {
            containerUnderTest().test(this) {
                containerHost.onIntent(PairingIntent.StartJoinFlow)
                expectState { copy(isJoinFlowChosen = true) }
                containerHost.onIntent(PairingIntent.CodeInputChanged("AB"))
                expectState { copy(codeInput = "AB") }
                containerHost.onIntent(PairingIntent.BackToChoice)
                expectState { copy(isJoinFlowChosen = false, codeInput = "") }
            }
        }

    @Test
    fun `code input is uppercased trimmed and capped at code length`() =
        runTest {
            containerUnderTest().test(this) {
                containerHost.onIntent(PairingIntent.CodeInputChanged(" abcd23x "))
                expectState { copy(codeInput = "ABCD23") }
            }
        }

    @Test
    fun `typing clears a previous join error`() =
        runTest {
            repository.nextJoinResult = JoinTripResult.InvalidCode

            containerUnderTest().test(this) {
                containerHost.onIntent(PairingIntent.StartJoinFlow)
                expectState { copy(isJoinFlowChosen = true) }
                containerHost.onIntent(PairingIntent.CodeInputChanged("ZZZZ99"))
                expectState { copy(codeInput = "ZZZZ99") }
                containerHost.onIntent(PairingIntent.SubmitCode)
                expectState { copy(isJoining = true) }
                expectState { copy(isJoining = false, joinError = PairingJoinError.INVALID_CODE) }
                containerHost.onIntent(PairingIntent.CodeInputChanged("ZZZZ9"))
                expectState { copy(codeInput = "ZZZZ9", joinError = null) }
            }
        }

    @Test
    fun `submitting a valid code fires Paired via the active-trip observation`() =
        runTest {
            val joinedTrip = fakeTrip(ownerId = "partner", memberId = "uid-1")
            repository.nextJoinResult = JoinTripResult.Joined(joinedTrip)

            containerUnderTest().test(this) {
                runOnCreate()
                expectState { copy(isCheckingTrip = false) }
                containerHost.onIntent(PairingIntent.StartJoinFlow)
                expectState { copy(isJoinFlowChosen = true) }
                containerHost.onIntent(PairingIntent.CodeInputChanged("ABCD23"))
                expectState { copy(codeInput = "ABCD23") }
                containerHost.onIntent(PairingIntent.SubmitCode)
                expectState { copy(isJoining = true) }
                expectState { copy(isJoining = false) }
                expectSideEffect(PairingSideEffect.Paired)
                cancelAndIgnoreRemainingItems()
            }
            assertEquals(listOf("ABCD23" to "uid-1"), repository.joinCalls)
        }

    @Test
    fun `submitting an unknown code shows the invalid-code error and stays on the join step`() =
        runTest {
            repository.nextJoinResult = JoinTripResult.InvalidCode

            containerUnderTest().test(this) {
                runOnCreate()
                expectState { copy(isCheckingTrip = false) }
                containerHost.onIntent(PairingIntent.StartJoinFlow)
                expectState { copy(isJoinFlowChosen = true) }
                containerHost.onIntent(PairingIntent.CodeInputChanged("ZZZZ99"))
                expectState { copy(codeInput = "ZZZZ99") }
                containerHost.onIntent(PairingIntent.SubmitCode)
                expectState { copy(isJoining = true) }
                expectState { copy(isJoining = false, joinError = PairingJoinError.INVALID_CODE) }
                cancelAndIgnoreRemainingItems()
            }
            assertEquals(
                PairingStep.JOIN_ENTER_CODE,
                PairingState(isJoinFlowChosen = true, joinError = PairingJoinError.INVALID_CODE).step,
            )
        }

    @Test
    fun `submitting your own code shows the own-code error`() =
        runTest {
            repository.nextJoinResult = JoinTripResult.OwnCode

            containerUnderTest().test(this) {
                containerHost.onIntent(PairingIntent.StartJoinFlow)
                expectState { copy(isJoinFlowChosen = true) }
                containerHost.onIntent(PairingIntent.CodeInputChanged("ABCD23"))
                expectState { copy(codeInput = "ABCD23") }
                containerHost.onIntent(PairingIntent.SubmitCode)
                expectState { copy(isJoining = true) }
                expectState { copy(isJoining = false, joinError = PairingJoinError.OWN_CODE) }
            }
        }

    @Test
    fun `submitting an already-used code shows the already-used error`() =
        runTest {
            repository.nextJoinResult = JoinTripResult.AlreadyUsed

            containerUnderTest().test(this) {
                containerHost.onIntent(PairingIntent.StartJoinFlow)
                expectState { copy(isJoinFlowChosen = true) }
                containerHost.onIntent(PairingIntent.CodeInputChanged("USED22"))
                expectState { copy(codeInput = "USED22") }
                containerHost.onIntent(PairingIntent.SubmitCode)
                expectState { copy(isJoining = true) }
                expectState { copy(isJoining = false, joinError = PairingJoinError.ALREADY_USED) }
            }
        }
}
