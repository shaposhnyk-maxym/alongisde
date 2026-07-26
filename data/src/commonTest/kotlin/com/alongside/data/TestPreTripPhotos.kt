package com.alongside.data

import com.alongside.core.model.SyncStatus
import com.alongside.core.model.pretrip.PreTripPhoto
import kotlin.time.Instant

internal fun testPreTripPhoto(
    id: String = "photo-1",
    tripId: String = "trip-1",
    userId: String = "owner-1",
    uri: String = "content://photos/photo-1",
    takenAt: Instant = Instant.fromEpochMilliseconds(1_752_600_000_000),
    latitude: Double = 49.8397,
    longitude: Double = 24.0297,
    remoteUrl: String? = null,
    syncStatus: SyncStatus = SyncStatus.PENDING,
): PreTripPhoto =
    PreTripPhoto(
        id = id,
        tripId = tripId,
        userId = userId,
        uri = uri,
        takenAt = takenAt,
        latitude = latitude,
        longitude = longitude,
        remoteUrl = remoteUrl,
        syncStatus = syncStatus,
    )
