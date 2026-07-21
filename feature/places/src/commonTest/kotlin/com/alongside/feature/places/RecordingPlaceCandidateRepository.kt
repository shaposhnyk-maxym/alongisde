package com.alongside.feature.places

import com.alongside.core.domain.place.PlaceCandidateRepository
import com.alongside.core.model.place.PlaceCandidate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** Map-backed fake that records upserts for assertions - the same shape as `data`'s equivalent. */
internal class RecordingPlaceCandidateRepository : PlaceCandidateRepository {
    private val places = MutableStateFlow<Map<String, PlaceCandidate>>(emptyMap())
    val upserted = mutableListOf<PlaceCandidate>()

    override suspend fun upsert(place: PlaceCandidate) {
        upserted += place
        places.value = places.value + (place.id to place)
    }

    override suspend fun getById(id: String): PlaceCandidate? = places.value[id]

    override fun observeByTrip(tripId: String): Flow<List<PlaceCandidate>> =
        places.map { all ->
            all.values.filter { it.tripId == tripId }
        }

    override suspend fun delete(id: String) {
        places.value = places.value - id
    }
}
