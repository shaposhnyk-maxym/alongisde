package com.alongside.core.model.pretrip

import com.alongside.core.model.SyncStatus
import kotlin.time.Instant

/**
 * A photo taken before the trip starts - not tied to any [com.alongside.core.model.diary.Episode]
 * or [com.alongside.core.model.diary.DiaryEntry] (there's no trip day yet to attach it to), so it
 * carries its own [tripId]/[userId]/[syncStatus] rather than inheriting them from a parent entity.
 */
public data class PreTripPhoto(
    val id: String,
    val tripId: String,
    val userId: String,
    val uri: String,
    val takenAt: Instant,
    val latitude: Double,
    val longitude: Double,
    val remoteUrl: String? = null,
    val syncStatus: SyncStatus,
)
