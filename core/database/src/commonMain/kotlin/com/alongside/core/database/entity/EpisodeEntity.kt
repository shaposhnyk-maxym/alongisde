package com.alongside.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.alongside.core.model.SyncStatus
import com.alongside.core.model.diary.Episode
import kotlin.time.Instant

@Entity(tableName = "episodes", indices = [Index("diaryEntryId")])
internal data class EpisodeEntity(
    @PrimaryKey val id: String,
    val diaryEntryId: String,
    val startTime: Instant,
    val endTime: Instant,
    val latitude: Double,
    val longitude: Double,
    val placeName: String?,
    val description: String?,
    val descriptionAttempts: Int,
    // defaultValue keeps the fresh CREATE TABLE shape identical to what MIGRATION_5_6's
    // ALTER TABLE ... DEFAULT 'PENDING' leaves behind.
    @ColumnInfo(defaultValue = "PENDING") val syncStatus: SyncStatus,
    @ColumnInfo(defaultValue = "0") val updatedAt: Instant,
    val city: String? = null,
    val cityPlaceId: String? = null,
    val countryCode: String? = null,
    @ColumnInfo(defaultValue = "0") val geocodeAttempts: Int = 0,
)

internal fun Episode.toEntity(): EpisodeEntity =
    EpisodeEntity(
        id = id,
        diaryEntryId = diaryEntryId,
        startTime = startTime,
        endTime = endTime,
        latitude = latitude,
        longitude = longitude,
        placeName = placeName,
        description = description,
        descriptionAttempts = descriptionAttempts,
        syncStatus = syncStatus,
        updatedAt = updatedAt,
        city = city,
        cityPlaceId = cityPlaceId,
        countryCode = countryCode,
        geocodeAttempts = geocodeAttempts,
    )
