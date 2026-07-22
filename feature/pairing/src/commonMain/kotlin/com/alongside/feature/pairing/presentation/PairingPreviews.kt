package com.alongside.feature.pairing.presentation

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alongside.core.model.SyncStatus
import com.alongside.core.model.trip.Trip
import com.alongside.core.ui.theme.AlongsideTheme
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

private val PreviewSize = Modifier.size(360.dp, 640.dp)

private val previewTrip =
    Trip(
        id = "trip-1",
        ownerId = "owner-1",
        memberId = null,
        inviteCode = "ABCD23",
        startDate = LocalDate(2026, 7, 18),
        endDate = LocalDate(2026, 8, 1),
        syncStatus = SyncStatus.PENDING,
        createdAt = Instant.fromEpochMilliseconds(0),
        updatedAt = Instant.fromEpochMilliseconds(0),
    )

@Composable
private fun PairingPreview(state: PairingState) {
    AlongsideTheme {
        PairingContent(
            // Settled end state - the active-trip check has always resolved by the time any of
            // these steps would actually be visible.
            state = state.copy(isCheckingTrip = false),
            onPickTripDates = {},
            onTripDatesChange = { _, _ -> },
            onConfirmTripDates = {},
            onStartJoinFlow = {},
            onBackToChoice = {},
            onCodeInputChange = {},
            onSubmitCode = {},
            modifier = PreviewSize,
            // Settled end state so the golden captures the finished layout, not the blank
            // pre-reveal frame of the entrance animation.
            animateEntrance = false,
        )
    }
}

@Preview
@Composable
private fun PairingChoicePreview() {
    PairingPreview(PairingState())
}

@Preview
@Composable
private fun PairingChoiceCreatingPreview() {
    PairingPreview(PairingState(isCreating = true))
}

@Preview
@Composable
private fun PairingCreatePickDatesPreview() {
    PairingPreview(
        PairingState(
            isPickingDates = true,
            tripStartDate = LocalDate(2026, 7, 18),
            tripEndDate = LocalDate(2026, 8, 1),
        ),
    )
}

@Preview
@Composable
private fun PairingCreateShowCodePreview() {
    PairingPreview(PairingState(ownTrip = previewTrip))
}

@Preview
@Composable
private fun PairingJoinEnterCodeEmptyPreview() {
    PairingPreview(PairingState(isJoinFlowChosen = true))
}

@Preview
@Composable
private fun PairingJoinEnterCodeFilledPreview() {
    PairingPreview(PairingState(isJoinFlowChosen = true, codeInput = "ABCD23"))
}

@Preview
@Composable
private fun PairingJoinSubmittingPreview() {
    PairingPreview(PairingState(isJoinFlowChosen = true, codeInput = "ABCD23", isJoining = true))
}

@Preview
@Composable
private fun PairingJoinErrorInvalidCodePreview() {
    PairingPreview(
        PairingState(
            isJoinFlowChosen = true,
            codeInput = "ZZZZ99",
            joinError = PairingJoinError.INVALID_CODE,
        ),
    )
}

@Preview
@Composable
private fun PairingJoinErrorOwnCodePreview() {
    PairingPreview(
        PairingState(
            isJoinFlowChosen = true,
            codeInput = "ABCD23",
            joinError = PairingJoinError.OWN_CODE,
        ),
    )
}

@Preview
@Composable
private fun PairingJoinErrorAlreadyUsedPreview() {
    PairingPreview(
        PairingState(
            isJoinFlowChosen = true,
            codeInput = "USED22",
            joinError = PairingJoinError.ALREADY_USED,
        ),
    )
}
