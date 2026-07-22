package com.alongside.feature.places.presentation

import com.alongside.core.domain.pairing.PairingRepository
import com.alongside.core.domain.place.PlaceContentPuller

/**
 * Worker-facing entry point (docs/roadmap.md, Matcher/Places pull-sync follow-up): gathers
 * [ownUserId]'s active trip, then delegates to [PlaceContentPuller] - there's no
 * Container/reactive state to read a trip id from in a background Worker, same trip-resolution
 * idiom `feature:diary`'s `DiaryCaptureCoordinator.retryAllIncompleteEpisodes` already uses.
 */
public class PlaceContentPullCoordinator(
    private val pairingRepository: PairingRepository,
    private val placeContentPuller: PlaceContentPuller,
) {
    public suspend fun pullActiveTripContent(ownUserId: String) {
        val trip = pairingRepository.getActiveTrip(ownUserId) ?: return
        placeContentPuller.pullTripContent(trip.id, ownUserId)
    }
}
