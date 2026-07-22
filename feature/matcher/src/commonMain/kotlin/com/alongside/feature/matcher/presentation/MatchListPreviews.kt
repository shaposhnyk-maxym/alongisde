package com.alongside.feature.matcher.presentation

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alongside.core.model.SyncStatus
import com.alongside.core.model.place.PlaceCandidate
import com.alongside.core.model.place.PlacePhoto
import com.alongside.core.model.place.PlaceSwipe
import com.alongside.core.model.place.SwipeDirection
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

private fun previewMatch(
    id: String,
    name: String,
    city: String?,
    rating: Double?,
    category: String?,
    countryCode: String? = null,
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
    photos = listOf(PlacePhoto(photoRef = "ref-1", remoteUrl = "https://storage/photo-1.jpg")),
    rating = rating,
    category = category,
    city = city,
    countryCode = countryCode,
)

private fun mutualLikeSwipes(candidateId: String) =
    listOf(
        previewSwipe(candidateId, "owner-1"),
        previewSwipe(candidateId, "member-1"),
    )

private fun previewSwipe(
    candidateId: String,
    userId: String,
) = PlaceSwipe(
    id = "$candidateId::$userId",
    tripId = "trip-1",
    candidateId = candidateId,
    userId = userId,
    direction = SwipeDirection.LIKE,
    swipedAt = Instant.fromEpochMilliseconds(0),
    syncStatus = SyncStatus.SYNCED,
    updatedAt = Instant.fromEpochMilliseconds(0),
)

private val PREVIEW_MATCHES =
    listOf(
        previewMatch(
            id = "1",
            name = "Rynok Square",
            city = "Lviv",
            rating = 4.8,
            category = "Landmark",
            countryCode = "UA",
        ),
        previewMatch(
            id = "2",
            name = "Lviv Coffee Mine",
            city = "Lviv",
            rating = 4.5,
            category = "Coffee shop",
            countryCode = "UA",
        ),
        previewMatch(
            id = "3",
            name = "Roshen Fountain",
            city = "Vinnytsia",
            rating = null,
            category = null,
            countryCode = "UA",
        ),
    )

@Composable
private fun MatchListPreview(state: MatcherState) {
    AlongsideTheme {
        MatchListContent(state = state, modifier = PreviewSize)
    }
}

@Preview
@Composable
private fun MatchListPopulatedPreview() {
    MatchListPreview(
        MatcherState(
            ownUserId = "owner-1",
            trip = PreviewTrip,
            candidates = PREVIEW_MATCHES,
            swipes = PREVIEW_MATCHES.flatMap { mutualLikeSwipes(it.id) },
        ),
    )
}

@Preview
@Composable
private fun MatchListEmptyPreview() {
    MatchListPreview(MatcherState(ownUserId = "owner-1", trip = PreviewTrip, candidates = emptyList()))
}
