package com.alongside.feature.places.presentation

import com.alongside.core.domain.pairing.PairingRepository
import com.alongside.core.domain.place.PlaceCandidateRepository
import com.alongside.core.model.place.PlaceCandidate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

/**
 * The Places tab's reactive read side: active trip -> that trip's places, straight from Room
 * (already kept in sync with Firestore by `SyncingPlaceCandidateRepository`/`SyncCoordinator`) -
 * the same trip-resolution idiom `PlaceRetryDataSource` already uses.
 */
public class PlacesListDataSource(
    private val pairingRepository: PairingRepository,
    private val placeCandidateRepository: PlaceCandidateRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    public suspend fun observe(
        ownUserId: String,
        onUpdate: suspend (List<PlaceCandidate>) -> Unit,
    ) {
        pairingRepository
            .observeActiveTrip(ownUserId)
            .flatMapLatest { trip ->
                trip?.let { placeCandidateRepository.observeByTrip(it.id) } ?: flowOf(emptyList())
            }.collect { onUpdate(it) }
    }
}
