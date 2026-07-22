package com.alongside.core.domain.place

import com.alongside.core.model.place.PlaceSwipe
import kotlinx.coroutines.flow.Flow

public interface PlaceSwipeRepository {
    public suspend fun upsert(swipe: PlaceSwipe)

    public suspend fun getById(id: String): PlaceSwipe?

    public fun observeByTrip(tripId: String): Flow<List<PlaceSwipe>>
}
