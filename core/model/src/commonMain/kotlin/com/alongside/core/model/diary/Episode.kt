package com.alongside.core.model.diary

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
    val photos: List<Photo>,
)
