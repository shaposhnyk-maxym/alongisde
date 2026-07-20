package com.alongside.data

import com.alongside.core.model.SyncStatus
import com.alongside.core.model.diary.Episode
import com.alongside.core.model.diary.Photo
import kotlin.time.Instant

internal fun testPhoto(
    id: String = "photo-1",
    uri: String = "content://photos/$id",
    takenAt: Instant = Instant.fromEpochMilliseconds(1_752_600_000_000),
    latitude: Double = 49.8397,
    longitude: Double = 24.0297,
    remoteUrl: String? = null,
): Photo =
    Photo(
        id = id,
        uri = uri,
        takenAt = takenAt,
        latitude = latitude,
        longitude = longitude,
        remoteUrl = remoteUrl,
    )

internal fun testEpisode(
    id: String = "episode-1",
    diaryEntryId: String = "entry-1",
    startTime: Instant = Instant.fromEpochMilliseconds(1_752_600_000_000),
    endTime: Instant = Instant.fromEpochMilliseconds(1_752_607_200_000),
    latitude: Double = 49.8397,
    longitude: Double = 24.0297,
    placeName: String? = "Rynok Square",
    description: String? = "Wandering the old town",
    descriptionAttempts: Int = 1,
    photos: List<Photo> = listOf(testPhoto()),
    syncStatus: SyncStatus = SyncStatus.PENDING,
    updatedAt: Instant = startTime,
): Episode =
    Episode(
        id = id,
        diaryEntryId = diaryEntryId,
        startTime = startTime,
        endTime = endTime,
        latitude = latitude,
        longitude = longitude,
        placeName = placeName,
        description = description,
        descriptionAttempts = descriptionAttempts,
        photos = photos,
        syncStatus = syncStatus,
        updatedAt = updatedAt,
    )
