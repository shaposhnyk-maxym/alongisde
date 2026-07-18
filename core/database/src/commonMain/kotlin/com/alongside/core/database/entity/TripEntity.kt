package com.alongside.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.alongside.core.model.SyncStatus
import com.alongside.core.model.trip.Trip
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

@Entity(tableName = "trips")
internal data class TripEntity(
    @PrimaryKey val id: String,
    val ownerId: String,
    val memberId: String?,
    val inviteCode: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val syncStatus: SyncStatus,
    val createdAt: Instant,
)

internal fun TripEntity.toDomain(): Trip =
    Trip(
        id = id,
        ownerId = ownerId,
        memberId = memberId,
        inviteCode = inviteCode,
        startDate = startDate,
        endDate = endDate,
        syncStatus = syncStatus,
        createdAt = createdAt,
        updatedAt = createdAt,
    )

internal fun Trip.toEntity(): TripEntity =
    TripEntity(
        id = id,
        ownerId = ownerId,
        memberId = memberId,
        inviteCode = inviteCode,
        startDate = startDate,
        endDate = endDate,
        syncStatus = syncStatus,
        createdAt = createdAt,
    )
