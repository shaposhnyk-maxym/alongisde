package com.alongside.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
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
    )
