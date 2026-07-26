package com.alongside.data.pretrip

import com.alongside.core.domain.pretrip.PreTripPhotoContentPuller
import com.alongside.core.domain.pretrip.PreTripPhotoRepository
import com.alongside.core.model.pretrip.PreTripPhoto
import com.alongside.core.network.firestore.FirestoreApi
import com.alongside.core.network.firestore.model.CollectionSelector
import com.alongside.core.network.firestore.model.FieldFilter
import com.alongside.core.network.firestore.model.FieldReference
import com.alongside.core.network.firestore.model.FirestoreValue
import com.alongside.core.network.firestore.model.QueryFilter
import com.alongside.core.network.firestore.model.StructuredQuery

/**
 * Real [PreTripPhotoContentPuller]: queries `preTripPhotos` by `tripId` and writes matches
 * straight into the LOCAL (non-syncing) repository - using the syncing decorator here would
 * re-enqueue a push of the partner's own document under this device's sync queue, which Firestore
 * rules reject outright (`update` requires the resource's own `userId` to equal `request.auth.uid`).
 * A single flat, `tripId`-scoped collection, unlike [com.alongside.data.diary.FirestoreDiaryContentPuller]'s
 * two-level entries-then-episodes query.
 */
public class FirestorePreTripPhotoContentPuller(
    private val api: FirestoreApi,
    private val localPreTripPhotoRepository: PreTripPhotoRepository,
) : PreTripPhotoContentPuller {
    override suspend fun pullTripContent(
        tripId: String,
        ownUserId: String,
    ) {
        val remotePhotos =
            api
                .runQuery(
                    StructuredQuery(
                        from = listOf(CollectionSelector(PreTripPhotoFirestoreMapper.COLLECTION_PATH)),
                        where = QueryFilter(fieldFilter = equalsFilter("tripId", tripId)),
                    ),
                ).map(PreTripPhotoFirestoreMapper::fromDocument)

        remotePhotos.forEach { photo ->
            if (photo.userId == ownUserId) {
                pullOwnIfMissingLocally(photo)
            } else {
                upsertIfChanged(photo)
            }
        }
    }

    // Own content only ever fills a local gap - never overwrites an existing local row, so a
    // pending local edit (e.g. a photo-retry upload) still in the sync queue is never clobbered.
    private suspend fun pullOwnIfMissingLocally(photo: PreTripPhoto) {
        if (localPreTripPhotoRepository.getById(photo.id) != null) return
        localPreTripPhotoRepository.upsert(photo)
    }

    // Partner content is safe to overwrite unconditionally (local storage never independently
    // edits it) - but re-writing the SAME value on every poll tick still touches Room, which
    // triggers Flow invalidation for every observer regardless of whether the row's content
    // actually changed. Skip the write entirely when the fetched value already matches local.
    private suspend fun upsertIfChanged(photo: PreTripPhoto) {
        if (localPreTripPhotoRepository.getById(photo.id) == photo) return
        localPreTripPhotoRepository.upsert(photo)
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
