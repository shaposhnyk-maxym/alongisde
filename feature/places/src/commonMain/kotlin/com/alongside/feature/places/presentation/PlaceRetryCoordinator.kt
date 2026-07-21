package com.alongside.feature.places.presentation

import com.alongside.core.domain.pairing.PairingRepository
import com.alongside.core.domain.place.PlaceCandidateRepository
import com.alongside.core.domain.place.importing.PlaceImportPipeline
import com.alongside.core.model.place.PlaceCandidate
import kotlinx.coroutines.flow.first

/**
 * Single source of truth for "does this place still need a retry" - shared by the coordinator
 * and [PlaceImportContainer].
 */
public fun PlaceCandidate.needsRetry(): Boolean = photos.any { it.remoteUrl == null }

/**
 * The `feature:places` analog of `feature:diary`'s `DiaryCaptureCoordinator` - place-import photo
 * retry, extracted from the now-deleted `PlaceRetryDataSource` in-memory poll loop
 * (docs/roadmap.md M12.11): WorkManager now drives this durably instead.
 */
public class PlaceRetryCoordinator(
    private val pairingRepository: PairingRepository,
    private val placeCandidateRepository: PlaceCandidateRepository,
    private val pipeline: PlaceImportPipeline,
) {
    /** Unit-testable independent of any polling/scheduling concern - only [places] actually needing it get retried. */
    public suspend fun retryIncompletePlaces(places: List<PlaceCandidate>) {
        for (place in places) {
            if (!place.needsRetry()) continue
            val retried = pipeline.retryIncomplete(place)
            if (retried != place) placeCandidateRepository.upsert(retried)
        }
    }

    /**
     * Worker-facing entry point (docs/roadmap.md M12.11): gathers [ownUserId]'s active trip's
     * places from scratch - there's no Container/reactive state to read from a background Worker.
     */
    public suspend fun retryAllIncompletePlaces(ownUserId: String) {
        val trip = pairingRepository.observeActiveTrip(ownUserId).first() ?: return
        val places = placeCandidateRepository.observeByTrip(trip.id).first()
        retryIncompletePlaces(places)
    }
}
