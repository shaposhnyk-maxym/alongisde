package com.alongside.feature.matcher.presentation

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alongside.core.model.SyncStatus
import com.alongside.core.model.place.PlaceCandidate
import com.alongside.core.model.trip.Trip
import com.alongside.core.ui.theme.AlongsideTheme
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

private val PreviewSize = Modifier.size(360.dp, 640.dp)

private val PreviewTrip =
    Trip(
        id = "trip-1",
        ownerId = "owner-1",
        memberId = "member-1",
        inviteCode = "ABCD23",
        startDate = LocalDate(2026, 7, 18),
        endDate = LocalDate(2026, 8, 1),
        syncStatus = SyncStatus.SYNCED,
        createdAt = Instant.fromEpochMilliseconds(0),
        updatedAt = Instant.fromEpochMilliseconds(0),
    )

private fun previewCandidate(
    id: String,
    name: String,
    city: String?,
) = PlaceCandidate(
    id = id,
    tripId = "trip-1",
    name = name,
    latitude = 49.8397,
    longitude = 24.0297,
    note = null,
    addedByUserId = "owner-1",
    syncStatus = SyncStatus.SYNCED,
    createdAt = Instant.fromEpochMilliseconds(0),
    updatedAt = Instant.fromEpochMilliseconds(0),
    city = city,
)

@Composable
private fun MatcherPreview(state: MatcherState) {
    AlongsideTheme {
        MatcherContent(state = state, onSwipe = { _, _ -> }, modifier = PreviewSize)
    }
}

@Preview
@Composable
private fun MatcherDeckCardPreview() {
    MatcherPreview(
        MatcherState(
            ownUserId = "owner-1",
            trip = PreviewTrip,
            candidates = listOf(previewCandidate("place-1", "Rynok Square", "Lviv")),
        ),
    )
}

@Preview
@Composable
private fun MatcherEmptyDeckPreview() {
    MatcherPreview(MatcherState(ownUserId = "owner-1", trip = PreviewTrip, candidates = emptyList()))
}
