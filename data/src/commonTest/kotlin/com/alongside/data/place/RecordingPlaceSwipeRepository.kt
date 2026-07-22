package com.alongside.data.place

import com.alongside.core.domain.place.PlaceSwipeRepository
import com.alongside.core.model.place.PlaceSwipe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** Map-backed local place swipes fake that records calls for assertions. */
internal class RecordingPlaceSwipeRepository : PlaceSwipeRepository {
    private val swipes = MutableStateFlow<Map<String, PlaceSwipe>>(emptyMap())
    val upserted = mutableListOf<PlaceSwipe>()

    override suspend fun upsert(swipe: PlaceSwipe) {
        upserted += swipe
        swipes.value = swipes.value + (swipe.id to swipe)
    }

    override suspend fun getById(id: String): PlaceSwipe? = swipes.value[id]

    override fun observeByTrip(tripId: String): Flow<List<PlaceSwipe>> =
        swipes.map { all ->
            all.values.filter { it.tripId == tripId }
        }
}
