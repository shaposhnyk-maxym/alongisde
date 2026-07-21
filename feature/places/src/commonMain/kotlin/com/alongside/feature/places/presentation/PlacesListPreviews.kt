package com.alongside.feature.places.presentation

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alongside.core.model.SyncStatus
import com.alongside.core.model.place.PlaceCandidate
import com.alongside.core.model.place.PlacePhoto
import com.alongside.core.ui.theme.AlongsideTheme
import kotlin.time.Instant

private val PreviewSize = Modifier.size(360.dp, 640.dp)

private fun previewPlace(
    id: String,
    name: String,
    city: String?,
    rating: Double? = null,
    category: String? = null,
) = PlaceCandidate(
    id = id,
    tripId = "trip-1",
    name = name,
    latitude = 49.8397,
    longitude = 24.0297,
    note = null,
    addedByUserId = "owner-1",
    ownerSwipe = null,
    memberSwipe = null,
    syncStatus = SyncStatus.PENDING,
    createdAt = Instant.fromEpochMilliseconds(0),
    updatedAt = Instant.fromEpochMilliseconds(0),
    photos = listOf(PlacePhoto(photoRef = "ref-1", remoteUrl = "https://storage/photo-1.jpg")),
    rating = rating,
    category = category,
    city = city,
)

private val PREVIEW_PLACES =
    listOf(
        previewPlace(id = "1", name = "Rynok Square", city = "Lviv", rating = 4.8, category = "Landmark"),
        previewPlace(id = "2", name = "Lviv Coffee Mine", city = "Lviv", rating = 4.5, category = "Coffee shop"),
        previewPlace(id = "3", name = "Roshen Fountain", city = "Vinnytsia", rating = 4.6),
        previewPlace(id = "4", name = "Global Solar", city = null),
    )

@Composable
private fun PlacesListPreview(state: PlacesListState) {
    AlongsideTheme {
        PlacesListContent(state = state, modifier = PreviewSize)
    }
}

@Preview
@Composable
private fun PlacesListLoadingPreview() {
    PlacesListPreview(PlacesListState(isLoading = true))
}

@Preview
@Composable
private fun PlacesListPopulatedPreview() {
    PlacesListPreview(PlacesListState(places = PREVIEW_PLACES, isLoading = false))
}

@Preview
@Composable
private fun PlacesListEmptyPreview() {
    PlacesListPreview(PlacesListState(places = emptyList(), isLoading = false))
}
