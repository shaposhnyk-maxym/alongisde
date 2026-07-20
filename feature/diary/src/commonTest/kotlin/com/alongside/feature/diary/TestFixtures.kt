package com.alongside.feature.diary

import com.alongside.core.domain.diary.DiaryContentPuller
import com.alongside.core.domain.diary.processing.EpisodeVisionDescriptionClient
import com.alongside.core.domain.diary.processing.GeocodingResult
import com.alongside.core.domain.diary.processing.PhotoUploadClient
import com.alongside.core.domain.diary.processing.PhotoUploadResult
import com.alongside.core.domain.diary.processing.PlaceGeocodingClient
import com.alongside.core.domain.diary.processing.VisionDescriptionResult
import com.alongside.core.model.SyncStatus
import com.alongside.core.model.diary.DiaryEntry
import com.alongside.core.model.diary.Photo
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

internal fun testDiaryEntry(
    id: String,
    tripId: String = "trip-1",
    userId: String = "uid-1",
    date: LocalDate = LocalDate(2026, 7, 19),
    syncStatus: SyncStatus = SyncStatus.SYNCED,
    closedAt: Instant? = null,
): DiaryEntry =
    DiaryEntry(
        id = id,
        tripId = tripId,
        userId = userId,
        date = date,
        syncStatus = syncStatus,
        createdAt = Instant.fromEpochMilliseconds(0),
        updatedAt = Instant.fromEpochMilliseconds(0),
        closedAt = closedAt,
    )

/** Always finds the same place, regardless of who's asking - mirrors the data-layer fixture. */
internal class FakeGeocodingClient : PlaceGeocodingClient {
    override suspend fun reverseGeocode(
        latitude: Double,
        longitude: Double,
    ): GeocodingResult = GeocodingResult.Found("Rynok Square")
}

internal class FakeVisionClient : EpisodeVisionDescriptionClient {
    override suspend fun describeEpisode(
        images: List<ByteArray>,
        placeName: String?,
        languageTag: String,
    ): VisionDescriptionResult = VisionDescriptionResult.Generated("A wander through the old town.")
}

internal class FakePhotoUploadClient : PhotoUploadClient {
    override suspend fun upload(
        photo: Photo,
        bytes: ByteArray,
    ): PhotoUploadResult = PhotoUploadResult.Uploaded("https://storage/${photo.id}")
}

/** No-op by default - tests that care about polling behavior can subclass or wrap this. */
internal class FakeDiaryContentPuller : DiaryContentPuller {
    val pulls = mutableListOf<Pair<String, String>>()

    override suspend fun pullTripContent(
        tripId: String,
        ownUserId: String,
    ) {
        pulls += tripId to ownUserId
    }
}
