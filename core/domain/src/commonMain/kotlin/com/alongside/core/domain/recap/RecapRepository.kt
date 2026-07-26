package com.alongside.core.domain.recap

import com.alongside.core.model.recap.Recap
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

public interface RecapRepository {
    public suspend fun ensureScheduled(
        tripId: String,
        availableAt: LocalDate,
    )

    public suspend fun getById(tripId: String): Recap?

    public fun observeById(tripId: String): Flow<Recap?>
}
