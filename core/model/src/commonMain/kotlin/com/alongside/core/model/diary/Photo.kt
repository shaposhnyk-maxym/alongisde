package com.alongside.core.model.diary

import kotlin.time.Instant

public data class Photo(
    val id: String,
    val uri: String,
    val takenAt: Instant,
    val latitude: Double,
    val longitude: Double,
    val remoteUrl: String? = null,
)
