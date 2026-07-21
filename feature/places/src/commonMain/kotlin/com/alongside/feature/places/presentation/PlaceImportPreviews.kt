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

private val previewPlace =
    PlaceCandidate(
        id = "place-1",
        tripId = "trip-1",
        name = "Lviv Coffee Manufacture",
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
        rating = 4.6,
        category = "Coffee shop",
    )

@Composable
private fun PlaceImportPreview(state: PlaceImportState) {
    AlongsideTheme {
        PlaceImportContent(state = state, onAccept = {}, onDiscard = {}, modifier = PreviewSize)
    }
}

@Preview
@Composable
private fun PlaceImportLoadingPreview() {
    PlaceImportPreview(PlaceImportState(status = PlaceImportStatus.LOADING))
}

@Preview
@Composable
private fun PlaceImportFoundPreview() {
    PlaceImportPreview(PlaceImportState(status = PlaceImportStatus.FOUND, place = previewPlace))
}

@Preview
@Composable
private fun PlaceImportNotFoundPreview() {
    PlaceImportPreview(PlaceImportState(status = PlaceImportStatus.NOT_FOUND))
}

@Preview
@Composable
private fun PlaceImportErrorPreview() {
    PlaceImportPreview(
        PlaceImportState(status = PlaceImportStatus.ERROR, errorMessage = "Something went wrong importing this place."),
    )
}
