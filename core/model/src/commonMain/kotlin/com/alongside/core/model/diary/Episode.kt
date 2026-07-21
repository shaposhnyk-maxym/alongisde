package com.alongside.core.model.diary

import com.alongside.core.model.SyncStatus
import kotlin.time.Instant

public data class Episode(
    val id: String,
    val diaryEntryId: String,
    val startTime: Instant,
    val endTime: Instant,
    val latitude: Double,
    val longitude: Double,
    val placeName: String?,
    val description: String?,
    val descriptionAttempts: Int,
    val photos: List<Photo>,
    val syncStatus: SyncStatus,
    val updatedAt: Instant,
    val city: String? = null,
    val cityPlaceId: String? = null,
    val countryCode: String? = null,
    val geocodeAttempts: Int = 0,
)
