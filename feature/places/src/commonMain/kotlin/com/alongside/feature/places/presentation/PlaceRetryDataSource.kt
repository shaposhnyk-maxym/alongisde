package com.alongside.feature.places.presentation

import com.alongside.core.domain.pairing.PairingRepository
import com.alongside.core.domain.place.PlaceCandidateRepository
import com.alongside.core.domain.place.importing.PlaceImportPipeline
import com.alongside.core.model.place.PlaceCandidate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlin.time.Duration.Companion.seconds

// Deliberately much less frequent than a cheap local query would need - this drives real Google
// Places photo/Storage upload calls, same reasoning as feature:diary's own retry poll interval.
private val INCOMPLETE_PHOTO_RETRY_POLL_INTERVAL = 30.seconds

/**
 * Background maintenance for place-import photos a capture-time hiccup left incomplete - the
 * `feature:places` analog of `feature:diary`'s `DiaryCaptureCoordinator.retryIncompleteEpisodes`
 * + `DiaryTimelineDataSource`'s poll loop. Same documented gap as that loop: [observeAndRetry]
 * only runs while whatever hosts it stays composed (currently the Places tab), not
 * process-lifetime durable - no WorkManager here either.
 */
public class PlaceRetryDataSource(
    private val pairingRepository: PairingRepository,
    private val placeCandidateRepository: PlaceCandidateRepository,
    private val pipeline: PlaceImportPipeline,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    public suspend fun observeAndRetry(ownUserId: String) {
        pairingRepository
            .observeActiveTrip(ownUserId)
            .map { it?.id }
            .distinctUntilChanged()
            .collectLatest { tripId -> if (tripId != null) pollTrip(tripId) }
    }

    private suspend fun pollTrip(tripId: String) {
        while (true) {
            delay(INCOMPLETE_PHOTO_RETRY_POLL_INTERVAL)
            val places = placeCandidateRepository.observeByTrip(tripId).first()
            runCatching { retryIncompletePlaces(places) }
                .onFailure { println("PlaceRetryDataSource: retry failed: $it") }
        }
    }

    /** Unit-testable independent of the poll loop's timing - only [places] actually needing it get retried. */
    public suspend fun retryIncompletePlaces(places: List<PlaceCandidate>) {
        for (place in places) {
            if (!place.needsRetry()) continue
            val retried = pipeline.retryIncomplete(place)
            if (retried != place) placeCandidateRepository.upsert(retried)
        }
    }

    private fun PlaceCandidate.needsRetry(): Boolean = photos.any { it.remoteUrl == null }
}
