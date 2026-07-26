package com.alongside.feature.recap

import com.alongside.core.model.SyncStatus
import com.alongside.core.model.diary.DiaryEntry
import com.alongside.core.model.diary.Episode
import com.alongside.core.model.place.PlaceCandidate
import com.alongside.core.model.place.PlaceSwipe
import com.alongside.core.model.place.SwipeDirection
import com.alongside.core.model.pretrip.PreTripPhoto
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

internal fun recapInstant(seconds: Long): Instant = Instant.fromEpochMilliseconds(seconds * 1000L)

internal fun recapDiaryEntry(
    id: String,
    tripId: String = "trip-1",
    userId: String,
    date: LocalDate,
): DiaryEntry =
    DiaryEntry(
        id = id,
        tripId = tripId,
        userId = userId,
        date = date,
        syncStatus = SyncStatus.SYNCED,
        createdAt = recapInstant(0),
        updatedAt = recapInstant(0),
    )

internal fun recapEpisode(
    id: String,
    diaryEntryId: String,
    startTime: Instant = recapInstant(0),
    endTime: Instant = recapInstant(0),
    latitude: Double = 0.0,
    longitude: Double = 0.0,
    city: String? = null,
): Episode =
    Episode(
        id = id,
        diaryEntryId = diaryEntryId,
        startTime = startTime,
        endTime = endTime,
        latitude = latitude,
        longitude = longitude,
        placeName = null,
        description = null,
        descriptionAttempts = 0,
        photos = emptyList(),
        syncStatus = SyncStatus.SYNCED,
        updatedAt = recapInstant(0),
        city = city,
    )

internal fun recapPreTripPhoto(
    id: String,
    tripId: String = "trip-1",
    userId: String,
    takenAt: Instant = recapInstant(0),
    latitude: Double = 0.0,
    longitude: Double = 0.0,
): PreTripPhoto =
    PreTripPhoto(
        id = id,
        tripId = tripId,
        userId = userId,
        uri = "content://$id",
        takenAt = takenAt,
        latitude = latitude,
        longitude = longitude,
        syncStatus = SyncStatus.SYNCED,
    )

internal fun recapPlaceCandidate(
    id: String,
    tripId: String = "trip-1",
    category: String? = null,
): PlaceCandidate =
    PlaceCandidate(
        id = id,
        tripId = tripId,
        name = "Place $id",
        latitude = 0.0,
        longitude = 0.0,
        note = null,
        addedByUserId = "uid-1",
        syncStatus = SyncStatus.SYNCED,
        createdAt = recapInstant(0),
        updatedAt = recapInstant(0),
        category = category,
    )

internal fun recapSwipe(
    id: String,
    tripId: String = "trip-1",
    candidateId: String,
    userId: String,
    direction: SwipeDirection,
): PlaceSwipe =
    PlaceSwipe(
        id = id,
        tripId = tripId,
        candidateId = candidateId,
        userId = userId,
        direction = direction,
        swipedAt = recapInstant(0),
        syncStatus = SyncStatus.SYNCED,
        updatedAt = recapInstant(0),
    )
