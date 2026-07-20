package com.alongside.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.alongside.core.model.diary.Photo
import kotlin.time.Instant

// Photo.id is the photo's content:// URI (see AndroidExifPhotoReader) - unique per physical file
// on one device, but nothing stops two DIFFERENT episodes from referencing the same physical
// file (reused test fixtures; eventually shared/imported photos). A single-column PK let
// upsertPhotos's INSERT OR REPLACE silently steal one episode's photo row for another episode
// on every poll tick (docs/roadmap.md M12.6 bugfix) - widened to (id, episodeId) so each episode
// owns its own row regardless of what id collides across episodes.
@Entity(
    tableName = "photos",
    primaryKeys = ["id", "episodeId"],
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
    val id: String,
    val episodeId: String,
    val uri: String,
    val takenAt: Instant,
    val latitude: Double,
    val longitude: Double,
    val remoteUrl: String? = null,
)

internal fun PhotoEntity.toDomain(): Photo =
    Photo(
        id = id,
        uri = uri,
        takenAt = takenAt,
        latitude = latitude,
        longitude = longitude,
        remoteUrl = remoteUrl,
    )

internal fun Photo.toEntity(episodeId: String): PhotoEntity =
    PhotoEntity(
        id = id,
        episodeId = episodeId,
        uri = uri,
        takenAt = takenAt,
        latitude = latitude,
        longitude = longitude,
        remoteUrl = remoteUrl,
    )
