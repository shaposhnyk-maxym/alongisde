package com.alongside.feature.settings.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.alongside.core.ui.component.CircleIconButton
import com.alongside.core.ui.component.InkGradientBackground
import com.alongside.core.ui.component.OverlineLabel
import com.alongside.core.ui.component.ScreenHeader
import com.alongside.core.ui.theme.AlongsideSpacing
import com.alongside.core.ui.theme.alongsideColors
import com.alongside.core.ui.theme.alongsideTypography
import org.orbitmvi.orbit.compose.collectAsState

@Composable
public fun SettingsScreen(
    container: SettingsContainer,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by container.collectAsState()

    SettingsContent(
        state = state,
        onLeaveTrip = { container.onIntent(SettingsIntent.RequestLeaveTrip) },
        onDeleteTrip = { container.onIntent(SettingsIntent.RequestDeleteTrip) },
        onConfirm = { container.onIntent(SettingsIntent.ConfirmPendingAction) },
        onDismissConfirmation = { container.onIntent(SettingsIntent.DismissConfirmation) },
        onClose = onClose,
        modifier = modifier,
    )
}

@Composable
internal fun SettingsContent(
    state: SettingsState,
    onLeaveTrip: () -> Unit,
    onDeleteTrip: () -> Unit,
    onConfirm: () -> Unit,
    onDismissConfirmation: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    InkGradientBackground(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            Column {
                ScreenHeader(title = "Settings") {
                    CircleIconButton(onClick = onClose, contentDescription = "Close settings") {
                        Text("✕")
                    }
                }
                if (!state.isLoading && state.trip != null) {
                    Column(modifier = Modifier.padding(horizontal = AlongsideSpacing.lg)) {
                        OverlineLabel(
                            text = "Trip",
                            modifier = Modifier.padding(start = AlongsideSpacing.md, bottom = AlongsideSpacing.xs),
                        )
                        TripSection(
                            isOwner = state.isOwner,
                            onLeaveTrip = onLeaveTrip,
                            onDeleteTrip = onDeleteTrip,
                        )
                    }
                }
            }
        }
    }

    val confirmation = state.pendingConfirmation
    if (confirmation != null) {
        SettingsConfirmationDialog(
            confirmation = confirmation,
            isProcessing = state.isProcessing,
            onConfirm = onConfirm,
            onDismiss = onDismissConfirmation,
        )
    }
}

@Composable
private fun TripSection(
    isOwner: Boolean,
    onLeaveTrip: () -> Unit,
    onDeleteTrip: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.alongsideColors.paperWhite,
        contentColor = MaterialTheme.alongsideColors.onPaper,
        shape = MaterialTheme.shapes.large,
    ) {
        Column {
            SettingsRow(text = "Leave Trip", color = MaterialTheme.colorScheme.primary, onClick = onLeaveTrip)
            if (isOwner) {
                HorizontalDivider(color = MaterialTheme.alongsideColors.onPaperSecondary.copy(alpha = 0.2f))
                SettingsRow(
                    text = "Delete Trip",
                    color = MaterialTheme.colorScheme.error,
                    trailing = "OWNER ONLY",
                    onClick = onDeleteTrip,
                )
            }
        }
    }
}

@Composable
private fun SettingsRow(
    text: String,
    color: Color,
    onClick: () -> Unit,
    trailing: String? = null,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = AlongsideSpacing.lg, vertical = AlongsideSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = text, color = color, modifier = Modifier.weight(1f))
        if (trailing != null) {
            Text(
                text = trailing,
                style = MaterialTheme.alongsideTypography.meta,
                color = MaterialTheme.alongsideColors.onPaperSecondary,
            )
        }
    }
}

@Composable
private fun SettingsConfirmationDialog(
    confirmation: SettingsConfirmation,
    isProcessing: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val isDelete = confirmation == SettingsConfirmation.DELETE_TRIP
    AlertDialog(
        // Not dismissible mid-flight: this is a real network write in progress (see
        // ConfirmedTripManagementRepository), not an offline-first optimistic one - the dialog
        // stays up with a spinner until the trip is confirmed gone, not just queued to become so.
        onDismissRequest = { if (!isProcessing) onDismiss() },
        title = { Text(if (isDelete) "Delete Trip?" else "Leave Trip?") },
        text = {
            Text(
                if (isDelete) {
                    "This permanently deletes the trip for both of you. This can't be undone."
                } else {
                    "You'll leave the trip. Your partner's data stays intact."
                },
            )
        },
        confirmButton = {
            if (isProcessing) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                TextButton(onClick = onConfirm) {
                    Text(
                        text = if (isDelete) "Delete" else "Leave",
                        color = if (isDelete) MaterialTheme.colorScheme.error else Color.Unspecified,
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isProcessing) { Text("Cancel") }
        },
    )
}
