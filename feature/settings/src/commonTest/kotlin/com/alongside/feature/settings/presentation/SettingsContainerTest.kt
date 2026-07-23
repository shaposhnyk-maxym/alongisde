package com.alongside.feature.settings.presentation

import com.alongside.core.domain.trip.DeleteTripResult
import com.alongside.core.domain.trip.LeaveTripResult
import com.alongside.feature.settings.FakeAuthSessionCache
import com.alongside.feature.settings.FakePairingRepository
import com.alongside.feature.settings.FakeTripManagementRepository
import com.alongside.feature.settings.fakeTrip
import com.alongside.feature.settings.testAuthSession
import kotlinx.coroutines.test.runTest
import org.orbitmvi.orbit.test.test
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsContainerTest {
    private val pairingRepository = FakePairingRepository()
    private val tripManagementRepository = FakeTripManagementRepository()

    private fun containerUnderTest(uid: String = "owner-1") =
        SettingsContainer(
            pairingRepository = pairingRepository,
            tripManagementRepository = tripManagementRepository,
            authSessionCache = FakeAuthSessionCache(testAuthSession(uid)),
        )

    @Test
    fun `the owner sees isOwner true`() =
        runTest {
            val trip = fakeTrip(ownerId = "owner-1", memberId = "member-1")
            pairingRepository.activeTrip.value = trip

            containerUnderTest(uid = "owner-1").test(this) {
                runOnCreate()
                expectState { copy(isLoading = false, trip = trip, currentUid = "owner-1") }
                cancelAndIgnoreRemainingItems()
            }
            assertEquals(true, SettingsState(trip = trip, currentUid = "owner-1").isOwner)
        }

    @Test
    fun `the member sees isOwner false`() =
        runTest {
            val trip = fakeTrip(ownerId = "owner-1", memberId = "member-1")
            pairingRepository.activeTrip.value = trip

            containerUnderTest(uid = "member-1").test(this) {
                runOnCreate()
                expectState { copy(isLoading = false, trip = trip, currentUid = "member-1") }
                cancelAndIgnoreRemainingItems()
            }
            assertEquals(false, SettingsState(trip = trip, currentUid = "member-1").isOwner)
        }

    @Test
    fun `a member requesting delete is rejected without opening a confirmation`() =
        runTest {
            val trip = fakeTrip(ownerId = "owner-1", memberId = "member-1")
            pairingRepository.activeTrip.value = trip

            containerUnderTest(uid = "member-1").test(this) {
                runOnCreate()
                expectState { copy(isLoading = false, trip = trip, currentUid = "member-1") }
                containerHost.onIntent(SettingsIntent.RequestDeleteTrip)
                cancelAndIgnoreRemainingItems()
            }
            assertEquals(emptyList(), tripManagementRepository.deleteCalls)
        }

    @Test
    fun `the owner requesting delete opens a confirmation`() =
        runTest {
            val trip = fakeTrip(ownerId = "owner-1", memberId = "member-1")
            pairingRepository.activeTrip.value = trip

            containerUnderTest(uid = "owner-1").test(this) {
                runOnCreate()
                expectState { copy(isLoading = false, trip = trip, currentUid = "owner-1") }
                containerHost.onIntent(SettingsIntent.RequestDeleteTrip)
                expectState { copy(pendingConfirmation = SettingsConfirmation.DELETE_TRIP) }
                cancelAndIgnoreRemainingItems()
            }
        }

    @Test
    fun `confirming a delete calls the repository and fires the side effect`() =
        runTest {
            val trip = fakeTrip(ownerId = "owner-1", memberId = "member-1")
            pairingRepository.activeTrip.value = trip
            tripManagementRepository.nextDeleteResult = DeleteTripResult.Deleted

            containerUnderTest(uid = "owner-1").test(this) {
                runOnCreate()
                expectState { copy(isLoading = false, trip = trip, currentUid = "owner-1") }
                containerHost.onIntent(SettingsIntent.RequestDeleteTrip)
                expectState { copy(pendingConfirmation = SettingsConfirmation.DELETE_TRIP) }
                containerHost.onIntent(SettingsIntent.ConfirmPendingAction)
                expectState { copy(isProcessing = true) }
                expectSideEffect(SettingsSideEffect.LeftOrDeletedTrip)
                expectState { copy(isProcessing = false, pendingConfirmation = null) }
                cancelAndIgnoreRemainingItems()
            }
            assertEquals(listOf(trip.id to "owner-1"), tripManagementRepository.deleteCalls)
        }

    @Test
    fun `confirming a leave calls the repository and fires the side effect`() =
        runTest {
            val trip = fakeTrip(ownerId = "owner-1", memberId = "member-1")
            pairingRepository.activeTrip.value = trip
            tripManagementRepository.nextLeaveResult = LeaveTripResult.Left(trip.copy(memberId = null))

            containerUnderTest(uid = "member-1").test(this) {
                runOnCreate()
                expectState { copy(isLoading = false, trip = trip, currentUid = "member-1") }
                containerHost.onIntent(SettingsIntent.RequestLeaveTrip)
                expectState { copy(pendingConfirmation = SettingsConfirmation.LEAVE_TRIP) }
                containerHost.onIntent(SettingsIntent.ConfirmPendingAction)
                expectState { copy(isProcessing = true) }
                expectSideEffect(SettingsSideEffect.LeftOrDeletedTrip)
                expectState { copy(isProcessing = false, pendingConfirmation = null) }
                cancelAndIgnoreRemainingItems()
            }
            assertEquals(listOf(trip.id to "member-1"), tripManagementRepository.leaveCalls)
        }

    @Test
    fun `a failed delete sync does not fire the side effect`() =
        runTest {
            val trip = fakeTrip(ownerId = "owner-1", memberId = "member-1")
            pairingRepository.activeTrip.value = trip
            tripManagementRepository.nextDeleteResult = DeleteTripResult.SyncFailed

            containerUnderTest(uid = "owner-1").test(this) {
                runOnCreate()
                expectState { copy(isLoading = false, trip = trip, currentUid = "owner-1") }
                containerHost.onIntent(SettingsIntent.RequestDeleteTrip)
                expectState { copy(pendingConfirmation = SettingsConfirmation.DELETE_TRIP) }
                containerHost.onIntent(SettingsIntent.ConfirmPendingAction)
                expectState { copy(isProcessing = true) }
                expectState { copy(isProcessing = false, pendingConfirmation = null) }
                cancelAndIgnoreRemainingItems()
            }
        }

    @Test
    fun `a failed leave sync does not fire the side effect`() =
        runTest {
            val trip = fakeTrip(ownerId = "owner-1", memberId = "member-1")
            pairingRepository.activeTrip.value = trip
            tripManagementRepository.nextLeaveResult = LeaveTripResult.SyncFailed

            containerUnderTest(uid = "member-1").test(this) {
                runOnCreate()
                expectState { copy(isLoading = false, trip = trip, currentUid = "member-1") }
                containerHost.onIntent(SettingsIntent.RequestLeaveTrip)
                expectState { copy(pendingConfirmation = SettingsConfirmation.LEAVE_TRIP) }
                containerHost.onIntent(SettingsIntent.ConfirmPendingAction)
                expectState { copy(isProcessing = true) }
                expectState { copy(isProcessing = false, pendingConfirmation = null) }
                cancelAndIgnoreRemainingItems()
            }
        }

    @Test
    fun `dismissing a confirmation clears it without calling the repository`() =
        runTest {
            val trip = fakeTrip(ownerId = "owner-1", memberId = "member-1")
            pairingRepository.activeTrip.value = trip

            containerUnderTest(uid = "owner-1").test(this) {
                runOnCreate()
                expectState { copy(isLoading = false, trip = trip, currentUid = "owner-1") }
                containerHost.onIntent(SettingsIntent.RequestDeleteTrip)
                expectState { copy(pendingConfirmation = SettingsConfirmation.DELETE_TRIP) }
                containerHost.onIntent(SettingsIntent.DismissConfirmation)
                expectState { copy(pendingConfirmation = null) }
                cancelAndIgnoreRemainingItems()
            }
            assertEquals(emptyList(), tripManagementRepository.deleteCalls)
        }
}
