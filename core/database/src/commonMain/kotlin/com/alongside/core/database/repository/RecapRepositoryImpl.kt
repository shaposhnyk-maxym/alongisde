package com.alongside.core.database.repository

import com.alongside.core.database.AlongsideDatabase
import com.alongside.core.database.entity.toDomain
import com.alongside.core.database.entity.toEntity
import com.alongside.core.domain.recap.RecapRepository
import com.alongside.core.model.recap.Recap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

internal class RecapRepositoryImpl(
    private val database: AlongsideDatabase,
) : RecapRepository {
    override suspend fun ensureScheduled(
        tripId: String,
        availableAt: LocalDate,
    ) {
        database.recapDao().ensureScheduled(Recap(tripId = tripId, availableAt = availableAt).toEntity())
    }

    override suspend fun getById(tripId: String): Recap? = database.recapDao().getById(tripId)?.toDomain()

    override fun observeById(tripId: String): Flow<Recap?> {
        val entityFlow = database.recapDao().observeById(tripId)
        return entityFlow.map { it?.toDomain() }
    }
}
