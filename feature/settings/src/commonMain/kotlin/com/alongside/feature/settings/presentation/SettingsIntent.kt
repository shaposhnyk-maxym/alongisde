package com.alongside.feature.settings.presentation

public sealed interface SettingsIntent {
    public data object RequestLeaveTrip : SettingsIntent

    public data object RequestDeleteTrip : SettingsIntent

    public data object ConfirmPendingAction : SettingsIntent

    public data object DismissConfirmation : SettingsIntent
}
