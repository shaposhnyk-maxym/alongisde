package com.alongside.feature.pairing.presentation

public sealed interface PairingIntent {
    public data object CreateTrip : PairingIntent

    public data object StartJoinFlow : PairingIntent

    public data object BackToChoice : PairingIntent

    public data class CodeInputChanged(
        val value: String,
    ) : PairingIntent

    public data object SubmitCode : PairingIntent
}
