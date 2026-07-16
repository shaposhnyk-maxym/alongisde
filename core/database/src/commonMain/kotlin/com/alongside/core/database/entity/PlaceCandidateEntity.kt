package com.alongside.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.alongside.core.model.SyncStatus
import com.alongside.core.model.place.PlaceCandidate
import com.alongside.core.model.place.SwipeDirection
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
    val ownerSwipe: SwipeDirection?,
    val memberSwipe: SwipeDirection?,
    val syncStatus: SyncStatus,
    val createdAt: Instant,
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
        ownerSwipe = ownerSwipe,
        memberSwipe = memberSwipe,
        syncStatus = syncStatus,
        createdAt = createdAt,
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
        ownerSwipe = ownerSwipe,
        memberSwipe = memberSwipe,
        syncStatus = syncStatus,
        createdAt = createdAt,
    )
