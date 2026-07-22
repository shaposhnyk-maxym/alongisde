package com.alongside.feature.places.presentation

import com.alongside.core.domain.pairing.PairingRepository
import com.alongside.core.domain.place.PlaceCandidateRepository
import com.alongside.core.domain.place.PlaceContentPuller
import com.alongside.core.model.place.PlaceCandidate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

private val TRIP_CONTENT_POLL_INTERVAL = 5.seconds

/**
 * The Places tab's reactive read side: active trip -> that trip's places, straight from Room
 * (already kept in sync with Firestore by `SyncingPlaceCandidateRepository`/`SyncCoordinator`) -
 * the same trip-resolution idiom `PlaceRetryDataSource` already uses. Also drives the remote
 * trip-content pull loop (partner-imported places, see [PlaceContentPuller]) while this screen is
 * open - the same idiom `feature:diary`'s `DiaryTimelineDataSource` already uses; the periodic
 * WorkManager sweep (`PlaceContentPullCoordinator`) is the backstop for when it isn't.
 */
public class PlacesListDataSource(
    private val pairingRepository: PairingRepository,
    private val placeCandidateRepository: PlaceCandidateRepository,
    private val placeContentPuller: PlaceContentPuller,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    public suspend fun observe(
        ownUserId: String,
        onUpdate: suspend (List<PlaceCandidate>) -> Unit,
    ) {
        val tripFlow = pairingRepository.observeActiveTrip(ownUserId)

        coroutineScope {
            // A second, independent collection of tripFlow - accepted duplicate polling of the
            // underlying pairing poll loop in exchange for clean cancellation: distinctUntilChanged
            // + collectLatest means exactly one trip-content poll loop runs at a time, automatically
            // replaced (not stacked) when the active trip changes, and stopped when it goes null.
            launch {
                tripFlow
                    .map { it?.id }
                    .distinctUntilChanged()
                    .collectLatest { tripId -> if (tripId != null) pollTripContent(tripId, ownUserId) }
            }

            tripFlow
                .flatMapLatest { trip ->
                    trip?.let { placeCandidateRepository.observeByTrip(it.id) } ?: flowOf(emptyList())
                }.collect { onUpdate(it) }
        }
    }

    private suspend fun pollTripContent(
        tripId: String,
        ownUserId: String,
    ) {
        while (true) {
            runCatching { placeContentPuller.pullTripContent(tripId, ownUserId) }
                .onFailure { println("PlacesListDataSource: pullTripContent failed: $it") }
            delay(TRIP_CONTENT_POLL_INTERVAL)
        }
    }
}
