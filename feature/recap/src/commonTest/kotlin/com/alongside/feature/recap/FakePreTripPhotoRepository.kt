package com.alongside.feature.recap

import com.alongside.core.domain.pretrip.PreTripPhotoRepository
import com.alongside.core.model.pretrip.PreTripPhoto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

internal class FakePreTripPhotoRepository : PreTripPhotoRepository {
    private val photos = MutableStateFlow<Map<String, PreTripPhoto>>(emptyMap())

    override suspend fun upsert(photo: PreTripPhoto) {
        photos.value = photos.value + (photo.id to photo)
    }

    override suspend fun getById(id: String): PreTripPhoto? = photos.value[id]

    override fun observeByTripAndUser(
        tripId: String,
        userId: String,
    ): Flow<List<PreTripPhoto>> = photos.map { it.values.filter { photo -> photo.tripId == tripId && photo.userId == userId } }

    override suspend fun delete(id: String) {
        photos.value = photos.value - id
    }
}
