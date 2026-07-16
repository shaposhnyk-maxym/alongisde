package com.alongside.core.database.repository

import com.alongside.core.database.AlongsideDatabase
import com.alongside.core.database.entity.toDomain
import com.alongside.core.database.entity.toEntity
import com.alongside.core.domain.place.PlaceCandidateRepository
import com.alongside.core.model.place.PlaceCandidate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class PlaceCandidateRepositoryImpl(
    private val database: AlongsideDatabase,
) : PlaceCandidateRepository {
    override suspend fun upsert(place: PlaceCandidate) {
        database.placeCandidateDao().upsert(place.toEntity())
    }

    override suspend fun getById(id: String): PlaceCandidate? = database.placeCandidateDao().getById(id)?.toDomain()

    override fun observeByTrip(tripId: String): Flow<List<PlaceCandidate>> =
        database.placeCandidateDao().observeByTrip(tripId).map { places -> places.map { it.toDomain() } }

    override suspend fun delete(id: String) {
        database.placeCandidateDao().delete(id)
    }
}
