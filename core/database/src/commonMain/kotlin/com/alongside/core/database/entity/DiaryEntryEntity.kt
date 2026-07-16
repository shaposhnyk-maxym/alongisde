package com.alongside.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.alongside.core.model.SyncStatus
import com.alongside.core.model.diary.DiaryEntry
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

@Entity(tableName = "diary_entries", indices = [Index("tripId")])
internal data class DiaryEntryEntity(
    @PrimaryKey val id: String,
    val tripId: String,
    val userId: String,
    val date: LocalDate,
    val syncStatus: SyncStatus,
    val createdAt: Instant,
)

internal fun DiaryEntryEntity.toDomain(): DiaryEntry =
    DiaryEntry(
        id = id,
        tripId = tripId,
        userId = userId,
        date = date,
        syncStatus = syncStatus,
        createdAt = createdAt,
    )

internal fun DiaryEntry.toEntity(): DiaryEntryEntity =
    DiaryEntryEntity(
        id = id,
        tripId = tripId,
        userId = userId,
        date = date,
        syncStatus = syncStatus,
        createdAt = createdAt,
    )
