package com.alongside.feature.recap

import com.alongside.core.domain.place.PlaceCandidateRepository
import com.alongside.core.model.place.PlaceCandidate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

internal class FakePlaceCandidateRepository : PlaceCandidateRepository {
    private val candidates = MutableStateFlow<Map<String, PlaceCandidate>>(emptyMap())

    override suspend fun upsert(place: PlaceCandidate) {
        candidates.value = candidates.value + (place.id to place)
    }

    override suspend fun getById(id: String): PlaceCandidate? = candidates.value[id]

    override fun observeByTrip(tripId: String): Flow<List<PlaceCandidate>> =
        candidates.map { it.values.filter { candidate -> candidate.tripId == tripId } }

    override suspend fun delete(id: String) {
        candidates.value = candidates.value - id
    }
}
