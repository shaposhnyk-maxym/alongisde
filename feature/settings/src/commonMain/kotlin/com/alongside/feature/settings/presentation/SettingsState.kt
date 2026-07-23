package com.alongside.feature.settings.presentation

import androidx.compose.runtime.Immutable
import com.alongside.core.model.trip.Trip

public enum class SettingsConfirmation {
    LEAVE_TRIP,
    DELETE_TRIP,
}

@Immutable
public data class SettingsState(
    val isLoading: Boolean = true,
    val trip: Trip? = null,
    val currentUid: String? = null,
    val pendingConfirmation: SettingsConfirmation? = null,
    val isProcessing: Boolean = false,
) {
    /** Derived, never stored, so it can never drift out of sync with [trip]/[currentUid]. */
    val isOwner: Boolean
        get() = trip != null && trip.ownerId == currentUid
}
