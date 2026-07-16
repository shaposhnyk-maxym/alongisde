package com.alongside.core.database.repository

import com.alongside.core.database.AlongsideDatabase
import com.alongside.core.database.entity.toDomain
import com.alongside.core.database.entity.toEntity
import com.alongside.core.domain.trip.TripRepository
import com.alongside.core.model.trip.Trip
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class TripRepositoryImpl(
    private val database: AlongsideDatabase,
) : TripRepository {
    override suspend fun upsert(trip: Trip) {
        database.tripDao().upsert(trip.toEntity())
    }

    override suspend fun getById(id: String): Trip? = database.tripDao().getById(id)?.toDomain()

    override fun observeById(id: String): Flow<Trip?> = database.tripDao().observeById(id).map { it?.toDomain() }

    override suspend fun delete(id: String) {
        database.tripDao().delete(id)
    }
}
