package com.alongside.core.model.diary

import com.alongside.core.model.SyncStatus
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

public data class DiaryEntry(
    val id: String,
    val tripId: String,
    val userId: String,
    val date: LocalDate,
    val syncStatus: SyncStatus,
    val createdAt: Instant,
)
