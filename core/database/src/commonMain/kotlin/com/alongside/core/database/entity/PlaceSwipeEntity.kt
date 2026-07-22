package com.alongside.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.alongside.core.model.SyncStatus
import com.alongside.core.model.place.PlaceSwipe
import com.alongside.core.model.place.SwipeDirection
import kotlin.time.Instant

@Entity(tableName = "place_swipes", indices = [Index("tripId")])
internal data class PlaceSwipeEntity(
    @PrimaryKey val id: String,
    val tripId: String,
    val candidateId: String,
    val userId: String,
    val direction: SwipeDirection,
    val swipedAt: Instant,
    val syncStatus: SyncStatus,
    val updatedAt: Instant,
)

internal fun PlaceSwipeEntity.toDomain(): PlaceSwipe =
    PlaceSwipe(
        id = id,
        tripId = tripId,
        candidateId = candidateId,
        userId = userId,
        direction = direction,
        swipedAt = swipedAt,
        syncStatus = syncStatus,
        updatedAt = updatedAt,
    )

internal fun PlaceSwipe.toEntity(): PlaceSwipeEntity =
    PlaceSwipeEntity(
        id = id,
        tripId = tripId,
        candidateId = candidateId,
        userId = userId,
        direction = direction,
        swipedAt = swipedAt,
        syncStatus = syncStatus,
        updatedAt = updatedAt,
    )
