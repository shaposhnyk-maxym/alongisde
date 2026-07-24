package com.alongside.core.domain.pretrip

import com.alongside.core.model.pretrip.PreTripPhoto
import kotlinx.coroutines.flow.Flow

public interface PreTripPhotoRepository {
    public suspend fun upsert(photo: PreTripPhoto)

    public suspend fun getById(id: String): PreTripPhoto?

    public fun observeByTripAndUser(
        tripId: String,
        userId: String,
    ): Flow<List<PreTripPhoto>>

    public suspend fun delete(id: String)
}
