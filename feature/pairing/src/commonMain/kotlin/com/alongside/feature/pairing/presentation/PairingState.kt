package com.alongside.feature.pairing.presentation

import androidx.compose.runtime.Immutable
import com.alongside.core.model.trip.Trip

public enum class PairingStep {
    CHOICE,
    CREATE_SHOW_CODE,
    JOIN_ENTER_CODE,
}

public enum class PairingJoinError {
    INVALID_CODE,
    OWN_CODE,
    ALREADY_USED,
}

@Immutable
public data class PairingState(
    val ownTrip: Trip? = null,
    val isJoinFlowChosen: Boolean = false,
    val isCreating: Boolean = false,
    val isJoining: Boolean = false,
    val codeInput: String = "",
    val joinError: PairingJoinError? = null,
) {
    /** Derived, never stored, so it can never drift out of sync with the flags above. */
    val step: PairingStep
        get() =
            when {
                ownTrip != null -> PairingStep.CREATE_SHOW_CODE
                isJoinFlowChosen -> PairingStep.JOIN_ENTER_CODE
                else -> PairingStep.CHOICE
            }
}
