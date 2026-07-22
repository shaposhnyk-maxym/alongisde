package com.alongside.core.domain.place

/**
 * Pulls place content (candidates + swipes) for [tripId] from the remote store into local
 * storage. Without this seam, a paired device only ever sees its own place content:
 * `observeByTrip` (on both `PlaceCandidateRepository` and `PlaceSwipeRepository`) is a plain
 * local Room query with no remote listener behind it - `SyncCoordinator` only ever pushes this
 * device's own pending writes.
 *
 * Partner-authored records are always overwritten from remote on every call - local storage never
 * independently edits the partner's documents, so there's nothing to protect. Own-authored
 * records are only ever pulled in to fill a genuine local gap: an own record that already exists
 * locally, in any state, is left untouched, so this can never clobber a pending local edit still
 * working its way through the sync queue (e.g. a photo-upload retry on a `PlaceCandidate`, or a
 * swipe not yet pushed).
 */
public interface PlaceContentPuller {
    public suspend fun pullTripContent(
        tripId: String,
        ownUserId: String,
    )
}
