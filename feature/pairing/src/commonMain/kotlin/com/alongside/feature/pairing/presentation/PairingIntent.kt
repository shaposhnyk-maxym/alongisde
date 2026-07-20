package com.alongside.feature.pairing.presentation

import kotlinx.datetime.LocalDate

public sealed interface PairingIntent {
    public data object PickTripDates : PairingIntent

    public data class TripDatesChanged(
        val startDate: LocalDate,
        val endDate: LocalDate,
    ) : PairingIntent

    public data object ConfirmTripDates : PairingIntent

    public data object StartJoinFlow : PairingIntent

    public data object BackToChoice : PairingIntent

    public data class CodeInputChanged(
        val value: String,
    ) : PairingIntent

    public data object SubmitCode : PairingIntent
}
