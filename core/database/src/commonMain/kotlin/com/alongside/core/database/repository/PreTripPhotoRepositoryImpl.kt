package com.alongside.core.database.repository

import com.alongside.core.database.AlongsideDatabase
import com.alongside.core.database.entity.toDomain
import com.alongside.core.database.entity.toEntity
import com.alongside.core.domain.pretrip.PreTripPhotoRepository
import com.alongside.core.model.pretrip.PreTripPhoto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class PreTripPhotoRepositoryImpl(
    private val database: AlongsideDatabase,
) : PreTripPhotoRepository {
    override suspend fun upsert(photo: PreTripPhoto) {
        database.preTripPhotoDao().upsert(photo.toEntity())
    }

    override suspend fun getById(id: String): PreTripPhoto? = database.preTripPhotoDao().getById(id)?.toDomain()

    override fun observeByTripAndUser(
        tripId: String,
        userId: String,
    ): Flow<List<PreTripPhoto>> =
        database.preTripPhotoDao().observeByTripAndUser(tripId, userId).map { photos -> photos.map { it.toDomain() } }

    override suspend fun delete(id: String) {
        database.preTripPhotoDao().delete(id)
    }
}
