package com.alongside.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.alongside.core.model.SyncStatus
import com.alongside.core.model.place.PlaceCandidate
import com.alongside.core.model.place.PlacePhoto
import kotlin.time.Instant

@Entity(tableName = "place_candidates", indices = [Index("tripId")])
internal data class PlaceCandidateEntity(
    @PrimaryKey val id: String,
    val tripId: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val note: String?,
    val addedByUserId: String,
    val syncStatus: SyncStatus,
    val createdAt: Instant,
    @ColumnInfo(defaultValue = "0") val updatedAt: Instant,
    @ColumnInfo(defaultValue = "''") val photos: List<PlacePhoto> = emptyList(),
    val rating: Double? = null,
    val category: String? = null,
    val city: String? = null,
    val cityPlaceId: String? = null,
    val countryCode: String? = null,
)

internal fun PlaceCandidateEntity.toDomain(): PlaceCandidate =
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

internal fun PlaceCandidate.toEntity(): PlaceCandidateEntity =
    PlaceCandidateEntity(
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
