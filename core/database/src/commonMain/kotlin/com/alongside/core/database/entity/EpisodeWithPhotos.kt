package com.alongside.core.database.entity

import androidx.room.Embedded
import androidx.room.Relation
import com.alongside.core.model.diary.Episode

internal data class EpisodeWithPhotos(
    @Embedded val episode: EpisodeEntity,
    @Relation(parentColumn = "id", entityColumn = "episodeId")
    val photos: List<PhotoEntity>,
)

internal fun EpisodeWithPhotos.toDomain(): Episode =
    Episode(
        id = episode.id,
        diaryEntryId = episode.diaryEntryId,
        startTime = episode.startTime,
        endTime = episode.endTime,
        latitude = episode.latitude,
        longitude = episode.longitude,
        placeName = episode.placeName,
        description = episode.description,
        descriptionAttempts = episode.descriptionAttempts,
        photos = photos.map { it.toDomain() },
        syncStatus = episode.syncStatus,
        updatedAt = episode.updatedAt,
        city = episode.city,
        cityPlaceId = episode.cityPlaceId,
        countryCode = episode.countryCode,
    )
