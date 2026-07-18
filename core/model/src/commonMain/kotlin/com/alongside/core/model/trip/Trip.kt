package com.alongside.core.model.trip

import com.alongside.core.model.SyncStatus
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

public data class Trip(
    val id: String,
    val ownerId: String,
    val memberId: String?,
    val inviteCode: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val syncStatus: SyncStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
)
