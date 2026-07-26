package com.alongside.feature.recap

import com.alongside.core.domain.place.PlaceSwipeRepository
import com.alongside.core.model.place.PlaceSwipe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

internal class FakePlaceSwipeRepository : PlaceSwipeRepository {
    private val swipes = MutableStateFlow<Map<String, PlaceSwipe>>(emptyMap())

    override suspend fun upsert(swipe: PlaceSwipe) {
        swipes.value = swipes.value + (swipe.id to swipe)
    }

    override suspend fun getById(id: String): PlaceSwipe? = swipes.value[id]

    override fun observeByTrip(tripId: String): Flow<List<PlaceSwipe>> = swipes.map { it.values.filter { swipe -> swipe.tripId == tripId } }
}
