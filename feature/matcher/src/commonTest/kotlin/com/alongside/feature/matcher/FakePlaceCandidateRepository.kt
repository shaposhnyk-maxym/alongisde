package com.alongside.feature.matcher

import com.alongside.core.domain.place.PlaceCandidateRepository
import com.alongside.core.model.SyncStatus
import com.alongside.core.model.place.PlaceCandidate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlin.time.Instant

internal fun fakeCandidate(
    id: String,
    tripId: String = "trip-1",
    name: String = "Rynok Square",
): PlaceCandidate =
    PlaceCandidate(
        id = id,
        tripId = tripId,
        name = name,
        latitude = 49.8397,
        longitude = 24.0297,
        note = null,
        addedByUserId = "owner-1",
        syncStatus = SyncStatus.SYNCED,
        createdAt = Instant.fromEpochMilliseconds(0),
        updatedAt = Instant.fromEpochMilliseconds(0),
    )

/** Map-backed local place candidates fake - only [observeByTrip]/[upsert] are exercised by Matcher. */
internal class FakePlaceCandidateRepository : PlaceCandidateRepository {
    private val places = MutableStateFlow<Map<String, PlaceCandidate>>(emptyMap())

    fun seed(vararg candidates: PlaceCandidate) {
        places.value = places.value + candidates.associateBy { it.id }
    }

    override suspend fun upsert(place: PlaceCandidate) {
        places.value = places.value + (place.id to place)
    }

    override suspend fun getById(id: String): PlaceCandidate? = places.value[id]

    override fun observeByTrip(tripId: String): Flow<List<PlaceCandidate>> = places.map { it.forTrip(tripId) }

    override suspend fun delete(id: String) {
        places.value = places.value - id
    }
}

private fun Map<String, PlaceCandidate>.forTrip(tripId: String) = values.filter { it.tripId == tripId }
