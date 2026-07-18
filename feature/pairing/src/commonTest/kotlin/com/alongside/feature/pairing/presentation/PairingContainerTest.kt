package com.alongside.feature.pairing.presentation

import com.alongside.core.domain.pairing.JoinTripResult
import com.alongside.feature.pairing.FakeAuthSessionCache
import com.alongside.feature.pairing.FakePairingRepository
import com.alongside.feature.pairing.fakeTrip
import com.alongside.feature.pairing.testAuthSession
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DateTimeUnit
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
                expectState { copy(ownTrip = ownTrip) }
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
    fun `creating a trip shows progress and then the invite code`() =
        runTest {
            containerUnderTest().test(this) {
                runOnCreate()
                containerHost.onIntent(PairingIntent.CreateTrip)
                expectState { copy(isCreating = true) }
                val created = repository.createdTrips.single()
                expectState { copy(isCreating = false, ownTrip = created) }
                cancelAndIgnoreRemainingItems()
            }
            val created = repository.createdTrips.single()
            assertEquals(PairingStep.CREATE_SHOW_CODE, PairingState(ownTrip = created).step)
            assertEquals("uid-1", created.ownerId)
            val expectedStart = FixedClock.todayIn(TimeZone.currentSystemDefault())
            assertEquals(expectedStart, created.startDate)
            assertEquals(expectedStart.plus(14, DateTimeUnit.DAY), created.endDate)
        }

    @Test
    fun `partner joining while waiting fires Paired`() =
        runTest {
            containerUnderTest().test(this) {
                runOnCreate()
                containerHost.onIntent(PairingIntent.CreateTrip)
                expectState { copy(isCreating = true) }
                val created = repository.createdTrips.single()
                expectState { copy(isCreating = false, ownTrip = created) }

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
