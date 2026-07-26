package com.alongside.core.model.recap

import kotlinx.datetime.LocalDate

/**
 * Unlike every other synced entity, both devices derive an identical [availableAt] locally
 * from the trip's end date - nothing to push or reconcile, so no `syncStatus`/`createdAt`/
 * `updatedAt`.
 */
public data class Recap(
    val tripId: String,
    val availableAt: LocalDate,
)
