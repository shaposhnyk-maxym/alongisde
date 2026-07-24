package com.alongside.feature.settings.presentation

public sealed interface SettingsSideEffect {
    /** The caller left or deleted the trip - navigation can leave this screen. */
    public data object LeftOrDeletedTrip : SettingsSideEffect
}
