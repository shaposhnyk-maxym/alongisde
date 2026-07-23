package com.alongside.feature.settings.presentation

import androidx.lifecycle.ViewModel
import com.alongside.core.domain.auth.AuthSessionCache
import com.alongside.core.domain.pairing.PairingRepository
import com.alongside.core.domain.trip.DeleteTripResult
import com.alongside.core.domain.trip.LeaveTripResult
import com.alongside.core.domain.trip.TripManagementRepository
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.container

public class SettingsContainer(
    private val pairingRepository: PairingRepository,
    private val tripManagementRepository: TripManagementRepository,
    private val authSessionCache: AuthSessionCache,
) : ViewModel(),
    ContainerHost<SettingsState, SettingsSideEffect> {
    override val container: Container<SettingsState, SettingsSideEffect> =
        container(SettingsState()) { observeTrip() }

    public fun onIntent(intent: SettingsIntent) {
        when (intent) {
            SettingsIntent.RequestLeaveTrip -> requestConfirmation(SettingsConfirmation.LEAVE_TRIP)
            SettingsIntent.RequestDeleteTrip -> requestConfirmation(SettingsConfirmation.DELETE_TRIP)
            SettingsIntent.ConfirmPendingAction -> confirmPendingAction()
            SettingsIntent.DismissConfirmation -> dismissConfirmation()
        }
    }

    private suspend fun Syntax<SettingsState, SettingsSideEffect>.observeTrip() {
        val uid = authSessionCache.get()?.user?.uid
        if (uid == null) {
            reduce { state.copy(isLoading = false) }
            return
        }
        pairingRepository.observeActiveTrip(uid).collect { trip ->
            reduce { state.copy(isLoading = false, trip = trip, currentUid = uid) }
        }
    }

    // Defense in depth alongside the repository-level check in confirmPendingAction() and the
    // screen simply not rendering the Delete Trip row for a member.
    private fun requestConfirmation(confirmation: SettingsConfirmation) =
        intent {
            if (confirmation == SettingsConfirmation.DELETE_TRIP && !state.isOwner) return@intent
            reduce { state.copy(pendingConfirmation = confirmation) }
        }

    private fun dismissConfirmation() =
        intent {
            reduce { state.copy(pendingConfirmation = null) }
        }

    private fun confirmPendingAction() =
        intent {
            val confirmation = state.pendingConfirmation ?: return@intent
            val trip = state.trip ?: return@intent
            val uid = state.currentUid ?: return@intent
            reduce { state.copy(isProcessing = true) }
            when (confirmation) {
                SettingsConfirmation.DELETE_TRIP -> {
                    val result = tripManagementRepository.deleteTrip(trip.id, uid)
                    if (result is DeleteTripResult.Deleted) postSideEffect(SettingsSideEffect.LeftOrDeletedTrip)
                }
                SettingsConfirmation.LEAVE_TRIP -> {
                    val result = tripManagementRepository.leaveTrip(trip.id, uid)
                    val succeeded =
                        result is LeaveTripResult.Left ||
                            result is LeaveTripResult.OwnershipTransferred ||
                            result is LeaveTripResult.Deleted
                    if (succeeded) postSideEffect(SettingsSideEffect.LeftOrDeletedTrip)
                }
            }
            reduce { state.copy(isProcessing = false, pendingConfirmation = null) }
        }
}
