package com.alongside.feature.diary.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.alongside.core.model.SyncStatus
import com.alongside.core.model.pretrip.PreTripPhoto
import kotlin.time.Instant

private fun previewPreTripPhoto(id: String) =
    PreTripPhoto(
        id = id,
        tripId = "trip-1",
        userId = "user-$id",
        uri = "content://pretrip/$id",
        takenAt = Instant.fromEpochMilliseconds(0),
        latitude = 49.8397,
        longitude = 24.0297,
        syncStatus = SyncStatus.SYNCED,
    )

@Preview
@Composable
private fun CountdownEmptyPreview() {
    ItemPreview(DiaryTimelineItem.Countdown(daysUntilReunion = 7, ownPhotos = emptyList(), partnerPhotos = emptyList()))
}

@Preview
@Composable
private fun CountdownWithPhotosPreview() {
    ItemPreview(
        DiaryTimelineItem.Countdown(
            daysUntilReunion = 7,
            ownPhotos = listOf(previewPreTripPhoto("own-1"), previewPreTripPhoto("own-2")),
            partnerPhotos = listOf(previewPreTripPhoto("partner-1")),
        ),
    )
}
