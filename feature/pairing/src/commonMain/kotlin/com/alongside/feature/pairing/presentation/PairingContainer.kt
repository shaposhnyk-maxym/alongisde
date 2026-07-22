package com.alongside.feature.pairing.presentation

import androidx.lifecycle.ViewModel
import com.alongside.core.domain.auth.AuthSessionCache
import com.alongside.core.domain.pairing.INVITE_CODE_LENGTH
import com.alongside.core.domain.pairing.JoinTripResult
import com.alongside.core.domain.pairing.PairingRepository
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.container
import kotlin.time.Clock

/** Default range the date-picker step opens with, before the user adjusts it. */
internal const val DEFAULT_TRIP_LENGTH_DAYS: Int = 14

public class PairingContainer(
    private val pairingRepository: PairingRepository,
    private val authSessionCache: AuthSessionCache,
    private val clock: Clock = Clock.System,
) : ViewModel(),
    ContainerHost<PairingState, PairingSideEffect> {
    override val container: Container<PairingState, PairingSideEffect> =
        container(PairingState()) { observeActiveTrip() }

    public fun onIntent(intent: PairingIntent) {
        when (intent) {
            PairingIntent.PickTripDates -> pickTripDates()
            is PairingIntent.TripDatesChanged -> tripDatesChanged(intent.startDate, intent.endDate)
            PairingIntent.ConfirmTripDates -> confirmTripDates()
            PairingIntent.StartJoinFlow -> startJoinFlow()
            PairingIntent.BackToChoice -> backToChoice()
            is PairingIntent.CodeInputChanged -> codeInputChanged(intent.value)
            PairingIntent.SubmitCode -> submitCode()
        }
    }

    // The single source of "we are paired": both the owner waiting for the partner and the
    // joiner submitting a code end up here when the stored trip gains its second person.
    private suspend fun Syntax<PairingState, PairingSideEffect>.observeActiveTrip() {
        val uid = currentUid()
        if (uid == null) {
            reduce { state.copy(isCheckingTrip = false) }
            return
        }
        pairingRepository.observeActiveTrip(uid).collect { trip ->
            when {
                trip == null -> reduce { state.copy(isCheckingTrip = false) }
                trip.memberId != null -> postSideEffect(PairingSideEffect.Paired)
                trip.ownerId == uid -> reduce { state.copy(isCheckingTrip = false, ownTrip = trip) }
            }
        }
    }

    private fun pickTripDates() =
        intent {
            val today = clock.todayIn(TimeZone.currentSystemDefault())
            reduce {
                state.copy(
                    isPickingDates = true,
                    tripStartDate = today,
                    tripEndDate = today.plus(DEFAULT_TRIP_LENGTH_DAYS, DateTimeUnit.DAY),
                )
            }
        }

    private fun tripDatesChanged(
        startDate: LocalDate,
        endDate: LocalDate,
    ) = intent {
        reduce { state.copy(tripStartDate = startDate, tripEndDate = endDate) }
    }

    private fun confirmTripDates() =
        intent {
            val uid = currentUid() ?: return@intent
            val startDate = state.tripStartDate ?: return@intent
            val endDate = state.tripEndDate ?: return@intent
            if (endDate < startDate) return@intent
            reduce { state.copy(isCreating = true) }
            val trip = pairingRepository.createTrip(ownerId = uid, startDate = startDate, endDate = endDate)
            reduce { state.copy(isCreating = false, isPickingDates = false, ownTrip = trip) }
        }

    private fun startJoinFlow() =
        intent {
            reduce { state.copy(isJoinFlowChosen = true) }
        }

    private fun backToChoice() =
        intent {
            reduce {
                state.copy(
                    isJoinFlowChosen = false,
                    isPickingDates = false,
                    tripStartDate = null,
                    tripEndDate = null,
                    codeInput = "",
                    joinError = null,
                )
            }
        }

    private fun codeInputChanged(value: String) =
        intent {
            val normalized = value.trim().uppercase().take(INVITE_CODE_LENGTH)
            reduce { state.copy(codeInput = normalized, joinError = null) }
        }

    private fun submitCode() =
        intent {
            val uid = currentUid() ?: return@intent
            reduce { state.copy(isJoining = true, joinError = null) }
            val result = pairingRepository.joinTrip(state.codeInput, uid)
            reduce { state.copy(isJoining = false, joinError = result.toErrorOrNull()) }
            // On Joined the active-trip observation sees the saved trip and fires Paired.
        }

    private suspend fun currentUid(): String? = authSessionCache.get()?.user?.uid
}

private fun JoinTripResult.toErrorOrNull(): PairingJoinError? =
    when (this) {
        is JoinTripResult.Joined -> null
        JoinTripResult.InvalidCode -> PairingJoinError.INVALID_CODE
        JoinTripResult.OwnCode -> PairingJoinError.OWN_CODE
        JoinTripResult.AlreadyUsed -> PairingJoinError.ALREADY_USED
    }
