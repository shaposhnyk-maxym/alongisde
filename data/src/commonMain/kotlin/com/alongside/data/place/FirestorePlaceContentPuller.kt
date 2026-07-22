package com.alongside.data.place

import com.alongside.core.domain.place.PlaceCandidateRepository
import com.alongside.core.domain.place.PlaceContentPuller
import com.alongside.core.domain.place.PlaceSwipeRepository
import com.alongside.core.model.place.PlaceCandidate
import com.alongside.core.model.place.PlaceSwipe
import com.alongside.core.network.firestore.FirestoreApi
import com.alongside.core.network.firestore.model.CollectionSelector
import com.alongside.core.network.firestore.model.FieldFilter
import com.alongside.core.network.firestore.model.FieldReference
import com.alongside.core.network.firestore.model.FirestoreValue
import com.alongside.core.network.firestore.model.QueryFilter
import com.alongside.core.network.firestore.model.StructuredQuery

/**
 * Real [PlaceContentPuller]: queries `placeCandidates`/`placeSwipes` by `tripId` and writes
 * matches straight into the LOCAL (non-syncing) repositories - using the syncing decorators here
 * would re-enqueue a push of the partner's own document under this device's sync queue, which
 * Firestore rules reject outright (`create`/`update` require the resource's own owner field to
 * equal `request.auth.uid`). Both collections are flat and `tripId`-scoped already, unlike
 * [com.alongside.data.diary.FirestoreDiaryContentPuller]'s two-level entries-then-episodes query.
 */
public class FirestorePlaceContentPuller(
    private val api: FirestoreApi,
    private val localPlaceCandidateRepository: PlaceCandidateRepository,
    private val localPlaceSwipeRepository: PlaceSwipeRepository,
) : PlaceContentPuller {
    override suspend fun pullTripContent(
        tripId: String,
        ownUserId: String,
    ) {
        pullCandidates(tripId, ownUserId)
        pullSwipes(tripId, ownUserId)
    }

    private suspend fun pullCandidates(
        tripId: String,
        ownUserId: String,
    ) {
        val remoteCandidates =
            api
                .runQuery(
                    StructuredQuery(
                        from = listOf(CollectionSelector(PlaceCandidateFirestoreMapper.COLLECTION_PATH)),
                        where = QueryFilter(fieldFilter = equalsFilter("tripId", tripId)),
                    ),
                ).map(PlaceCandidateFirestoreMapper::fromDocument)

        remoteCandidates.forEach { candidate ->
            if (candidate.addedByUserId == ownUserId) {
                pullOwnCandidateIfMissingLocally(candidate)
            } else {
                upsertCandidateIfChanged(candidate)
            }
        }
    }

    private suspend fun pullSwipes(
        tripId: String,
        ownUserId: String,
    ) {
        val remoteSwipes =
            api
                .runQuery(
                    StructuredQuery(
                        from = listOf(CollectionSelector(PlaceSwipeFirestoreMapper.COLLECTION_PATH)),
                        where = QueryFilter(fieldFilter = equalsFilter("tripId", tripId)),
                    ),
                ).map(PlaceSwipeFirestoreMapper::fromDocument)

        remoteSwipes.forEach { swipe ->
            if (swipe.userId == ownUserId) {
                pullOwnSwipeIfMissingLocally(swipe)
            } else {
                upsertSwipeIfChanged(swipe)
            }
        }
    }

    // Own content only ever fills a local gap - never overwrites an existing local row, so a
    // pending local edit (e.g. a photo-retry upload) still in the sync queue is never clobbered.
    private suspend fun pullOwnCandidateIfMissingLocally(candidate: PlaceCandidate) {
        if (localPlaceCandidateRepository.getById(candidate.id) != null) return
        localPlaceCandidateRepository.upsert(candidate)
    }

    private suspend fun pullOwnSwipeIfMissingLocally(swipe: PlaceSwipe) {
        if (localPlaceSwipeRepository.getById(swipe.id) != null) return
        localPlaceSwipeRepository.upsert(swipe)
    }

    // Partner content is safe to overwrite unconditionally (local storage never independently
    // edits it) - but re-writing the SAME value on every poll tick still touches Room, which
    // triggers Flow invalidation for every observer regardless of whether the row's content
    // actually changed. Skip the write entirely when the fetched value already matches local.
    private suspend fun upsertCandidateIfChanged(candidate: PlaceCandidate) {
        if (localPlaceCandidateRepository.getById(candidate.id) == candidate) return
        localPlaceCandidateRepository.upsert(candidate)
    }

    private suspend fun upsertSwipeIfChanged(swipe: PlaceSwipe) {
        if (localPlaceSwipeRepository.getById(swipe.id) == swipe) return
        localPlaceSwipeRepository.upsert(swipe)
    }

    private fun equalsFilter(
        fieldPath: String,
        value: String,
    ): FieldFilter =
        FieldFilter(
            field = FieldReference(fieldPath),
            op = "EQUAL",
            value = FirestoreValue.StringValue(value),
        )
}
