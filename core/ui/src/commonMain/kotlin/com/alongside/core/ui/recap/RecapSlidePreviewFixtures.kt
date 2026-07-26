package com.alongside.core.ui.recap

import com.alongside.core.model.SyncStatus
import com.alongside.core.model.diary.Episode
import com.alongside.core.model.place.PlaceCandidate
import com.alongside.core.model.pretrip.PreTripPhoto
import kotlin.time.Instant

/**
 * Minimal fixture builders for this package's `@Preview`s (screenshot-tested via Roborazzi -
 * docs/roadmap.md M20.3.5). Mirrors `core:domain`'s `RecapTestFixtures.kt`/`feature:diary`'s
 * `CountdownPreviews.kt` convention - `core:ui` can't see `core:domain`'s `internal` test
 * fixtures, so this duplicates the minimal shape rather than exposing new public API for it.
 */
internal fun recapPreviewInstant(seconds: Long = 0) = Instant.fromEpochMilliseconds(seconds * 1000L)

internal fun recapPreviewPreTripPhoto(id: String) =
    PreTripPhoto(
        id = id,
        tripId = "trip-1",
        userId = "user-$id",
        uri = "content://pretrip/$id",
        takenAt = recapPreviewInstant(),
        latitude = 49.8397,
        longitude = 24.0297,
        syncStatus = SyncStatus.SYNCED,
    )

internal fun recapPreviewEpisode(
    id: String,
    description: String? = null,
    city: String? = null,
) = Episode(
    id = id,
    diaryEntryId = "entry-$id",
    startTime = recapPreviewInstant(),
    endTime = recapPreviewInstant(3600),
    latitude = 49.55,
    longitude = 25.6,
    placeName = null,
    description = description,
    descriptionAttempts = 0,
    photos = emptyList(),
    syncStatus = SyncStatus.SYNCED,
    updatedAt = recapPreviewInstant(),
    city = city,
)

internal fun recapPreviewPlaceCandidate(
    id: String,
    name: String,
    category: String? = null,
) = PlaceCandidate(
    id = id,
    tripId = "trip-1",
    name = name,
    latitude = 0.0,
    longitude = 0.0,
    note = null,
    addedByUserId = "own",
    syncStatus = SyncStatus.SYNCED,
    createdAt = recapPreviewInstant(),
    updatedAt = recapPreviewInstant(),
    category = category,
)
