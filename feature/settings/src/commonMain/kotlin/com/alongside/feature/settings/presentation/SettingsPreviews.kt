package com.alongside.feature.settings.presentation

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
        memberId = "member-1",
        inviteCode = "ABCD23",
        startDate = LocalDate(2026, 7, 18),
        endDate = LocalDate(2026, 8, 1),
        syncStatus = SyncStatus.PENDING,
        createdAt = Instant.fromEpochMilliseconds(0),
        updatedAt = Instant.fromEpochMilliseconds(0),
    )

@Composable
private fun SettingsPreview(state: SettingsState) {
    AlongsideTheme {
        SettingsContent(
            state = state,
            onLeaveTrip = {},
            onDeleteTrip = {},
            onConfirm = {},
            onDismissConfirmation = {},
            onClose = {},
            modifier = PreviewSize,
        )
    }
}

@Preview
@Composable
private fun SettingsOwnerPreview() {
    SettingsPreview(SettingsState(isLoading = false, trip = previewTrip, currentUid = previewTrip.ownerId))
}

@Preview
@Composable
private fun SettingsMemberPreview() {
    SettingsPreview(SettingsState(isLoading = false, trip = previewTrip, currentUid = previewTrip.memberId))
}

@Preview
@Composable
private fun SettingsDeleteConfirmationPreview() {
    SettingsPreview(
        SettingsState(
            isLoading = false,
            trip = previewTrip,
            currentUid = previewTrip.ownerId,
            pendingConfirmation = SettingsConfirmation.DELETE_TRIP,
        ),
    )
}

@Preview
@Composable
private fun SettingsDeleteConfirmationProcessingPreview() {
    SettingsPreview(
        SettingsState(
            isLoading = false,
            trip = previewTrip,
            currentUid = previewTrip.ownerId,
            pendingConfirmation = SettingsConfirmation.DELETE_TRIP,
            isProcessing = true,
        ),
    )
}
