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
    val syncStatus: SyncStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
    val photos: List<PlacePhoto> = emptyList(),
    val rating: Double? = null,
    val category: String? = null,
    val city: String? = null,
    val cityPlaceId: String? = null,
    val countryCode: String? = null,
)
