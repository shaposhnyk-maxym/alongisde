package com.alongside.core.model.place

import com.alongside.core.model.SyncStatus
import kotlin.time.Instant

public data class PlaceCandidate(
    val id: String,
    val tripId: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val note: String?,
    val addedByUserId: String,
    val ownerSwipe: SwipeDirection?,
    val memberSwipe: SwipeDirection?,
    val syncStatus: SyncStatus,
    val createdAt: Instant,
)
