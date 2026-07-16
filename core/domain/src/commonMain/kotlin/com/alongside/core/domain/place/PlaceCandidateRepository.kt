package com.alongside.core.domain.place

import com.alongside.core.model.place.PlaceCandidate
import kotlinx.coroutines.flow.Flow

public interface PlaceCandidateRepository {
    public suspend fun upsert(place: PlaceCandidate)

    public suspend fun getById(id: String): PlaceCandidate?

    public fun observeByTrip(tripId: String): Flow<List<PlaceCandidate>>

    public suspend fun delete(id: String)
}
