package com.alongside.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.alongside.core.model.SyncStatus
import com.alongside.core.model.push.PushPlatform
import com.alongside.core.model.push.PushToken
import kotlin.time.Instant

@Entity(tableName = "push_tokens")
internal data class PushTokenEntity(
    @PrimaryKey val userId: String,
    val token: String,
    val platform: PushPlatform,
    val syncStatus: SyncStatus,
    val updatedAt: Instant,
)

internal fun PushTokenEntity.toDomain(): PushToken =
    PushToken(
        userId = userId,
        token = token,
        platform = platform,
        syncStatus = syncStatus,
        updatedAt = updatedAt,
    )

internal fun PushToken.toEntity(): PushTokenEntity =
    PushTokenEntity(
        userId = userId,
        token = token,
        platform = platform,
        syncStatus = syncStatus,
        updatedAt = updatedAt,
    )
