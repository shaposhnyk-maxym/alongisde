package com.alongside.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.alongside.core.model.diary.Photo
import kotlin.time.Instant

@Entity(
    tableName = "photos",
    indices = [Index("episodeId")],
    foreignKeys = [
        ForeignKey(
            entity = EpisodeEntity::class,
            parentColumns = ["id"],
            childColumns = ["episodeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
internal data class PhotoEntity(
    @PrimaryKey val id: String,
    val episodeId: String,
    val uri: String,
    val takenAt: Instant,
    val latitude: Double,
    val longitude: Double,
)

internal fun PhotoEntity.toDomain(): Photo =
    Photo(
        id = id,
        uri = uri,
        takenAt = takenAt,
        latitude = latitude,
        longitude = longitude,
    )

internal fun Photo.toEntity(episodeId: String): PhotoEntity =
    PhotoEntity(
        id = id,
        episodeId = episodeId,
        uri = uri,
        takenAt = takenAt,
        latitude = latitude,
        longitude = longitude,
    )
