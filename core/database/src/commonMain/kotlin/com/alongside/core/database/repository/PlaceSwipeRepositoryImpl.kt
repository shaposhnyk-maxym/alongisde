package com.alongside.core.database.repository

import com.alongside.core.database.AlongsideDatabase
import com.alongside.core.database.entity.toDomain
import com.alongside.core.database.entity.toEntity
import com.alongside.core.domain.place.PlaceSwipeRepository
import com.alongside.core.model.place.PlaceSwipe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class PlaceSwipeRepositoryImpl(
    private val database: AlongsideDatabase,
) : PlaceSwipeRepository {
    override suspend fun upsert(swipe: PlaceSwipe) {
        database.placeSwipeDao().upsert(swipe.toEntity())
    }

    override suspend fun getById(id: String): PlaceSwipe? = database.placeSwipeDao().getById(id)?.toDomain()

    override fun observeByTrip(tripId: String): Flow<List<PlaceSwipe>> =
        database.placeSwipeDao().observeByTrip(tripId).map { swipes -> swipes.map { it.toDomain() } }
}
