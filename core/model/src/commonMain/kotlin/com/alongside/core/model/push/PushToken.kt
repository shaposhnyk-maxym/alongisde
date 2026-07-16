package com.alongside.core.model.push

import com.alongside.core.model.SyncStatus
import kotlin.time.Instant

/**
 * One FCM registration token per user (`pushTokens/{userId}` in Firestore - a device
 * re-registering just overwrites it).
 */
public data class PushToken(
    val userId: String,
    val token: String,
    val platform: PushPlatform,
    val syncStatus: SyncStatus,
    val updatedAt: Instant,
)
