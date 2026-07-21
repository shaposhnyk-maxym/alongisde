package com.alongside.data

import com.alongside.core.model.SyncStatus
import com.alongside.core.model.place.PlaceCandidate
import com.alongside.core.model.place.PlacePhoto
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
    ownerSwipe: SwipeDirection? = null,
    memberSwipe: SwipeDirection? = null,
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
        ownerSwipe = ownerSwipe,
        memberSwipe = memberSwipe,
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
