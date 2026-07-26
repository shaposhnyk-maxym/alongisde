package com.alongside.core.domain.pretrip

/**
 * Pulls pre-trip photo content for [tripId] from the remote store into local storage. Without
 * this seam, a paired device only ever sees its own pre-trip photos: `observeByTripAndUser` is a
 * plain local Room query with no remote listener behind it.
 *
 * Partner-authored photos are always overwritten from remote on every call - local storage never
 * independently edits the partner's documents, so there's nothing to protect. Own-authored photos
 * are only ever pulled in to fill a genuine local gap: an own photo that already exists locally,
 * in any state, is left untouched, so this can never clobber a pending local edit still working
 * its way through the sync queue.
 */
public interface PreTripPhotoContentPuller {
    public suspend fun pullTripContent(
        tripId: String,
        ownUserId: String,
    )
}
