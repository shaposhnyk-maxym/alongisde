package com.alongside.data.pretrip

import com.alongside.core.domain.pretrip.PreTripPhotoRepository
import com.alongside.core.model.pretrip.PreTripPhoto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** Map-backed local pre-trip-photos fake that records calls for assertions. */
internal class RecordingPreTripPhotoRepository : PreTripPhotoRepository {
    private val photos = MutableStateFlow<Map<String, PreTripPhoto>>(emptyMap())
    val upserted = mutableListOf<PreTripPhoto>()
    val deletedIds = mutableListOf<String>()

    override suspend fun upsert(photo: PreTripPhoto) {
        upserted += photo
        photos.value = photos.value + (photo.id to photo)
    }

    override suspend fun getById(id: String): PreTripPhoto? = photos.value[id]

    override fun observeByTripAndUser(
        tripId: String,
        userId: String,
    ): Flow<List<PreTripPhoto>> = photos.map { all -> all.values.filter { it.tripId == tripId && it.userId == userId } }

    override suspend fun delete(id: String) {
        deletedIds += id
        photos.value = photos.value - id
    }
}
