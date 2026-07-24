package com.alongside.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.alongside.core.model.SyncStatus
import com.alongside.core.model.pretrip.PreTripPhoto
import kotlin.time.Instant

@Entity(tableName = "pre_trip_photos", indices = [Index("tripId", "userId")])
internal data class PreTripPhotoEntity(
    @PrimaryKey val id: String,
    val tripId: String,
    val userId: String,
    val uri: String,
    val takenAt: Instant,
    val latitude: Double,
    val longitude: Double,
    val remoteUrl: String?,
    val syncStatus: SyncStatus,
)

internal fun PreTripPhotoEntity.toDomain(): PreTripPhoto =
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

internal fun PreTripPhoto.toEntity(): PreTripPhotoEntity =
    PreTripPhotoEntity(
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
