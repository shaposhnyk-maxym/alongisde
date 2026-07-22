package com.alongside.data

import com.alongside.core.model.SyncStatus
import com.alongside.core.model.place.PlaceCandidate
import com.alongside.core.model.place.PlacePhoto
import com.alongside.core.model.place.PlaceSwipe
import com.alongside.core.model.place.SwipeDirection
import kotlin.time.Instant

internal fun testPlace(
    id: String = "place-1",
    tripId: String = "trip-1",
    name: String = "Lviv Coffee Manufacture",
    latitude: Double = 49.8397,
    longitude: Double = 24.0297,
    note: String? = null,
    addedByUserId: String = "owner-1",
    photos: List<PlacePhoto> = emptyList(),
    rating: Double? = null,
    category: String? = null,
    city: String? = null,
    cityPlaceId: String? = null,
    countryCode: String? = null,
    syncStatus: SyncStatus = SyncStatus.PENDING,
    createdAt: Instant = Instant.fromEpochMilliseconds(1_752_600_000_000),
    updatedAt: Instant = createdAt,
): PlaceCandidate =
    PlaceCandidate(
        id = id,
        tripId = tripId,
        name = name,
        latitude = latitude,
        longitude = longitude,
        note = note,
        addedByUserId = addedByUserId,
        syncStatus = syncStatus,
        createdAt = createdAt,
        updatedAt = updatedAt,
        photos = photos,
        rating = rating,
        category = category,
        city = city,
        cityPlaceId = cityPlaceId,
        countryCode = countryCode,
    )

internal fun testPlaceSwipe(
    id: String = "place-1::owner-1",
    tripId: String = "trip-1",
    candidateId: String = "place-1",
    userId: String = "owner-1",
    direction: SwipeDirection = SwipeDirection.LIKE,
    swipedAt: Instant = Instant.fromEpochMilliseconds(1_752_600_000_000),
    syncStatus: SyncStatus = SyncStatus.PENDING,
    updatedAt: Instant = swipedAt,
): PlaceSwipe =
    PlaceSwipe(
        id = id,
        tripId = tripId,
        candidateId = candidateId,
        userId = userId,
        direction = direction,
        swipedAt = swipedAt,
        syncStatus = syncStatus,
        updatedAt = updatedAt,
    )
