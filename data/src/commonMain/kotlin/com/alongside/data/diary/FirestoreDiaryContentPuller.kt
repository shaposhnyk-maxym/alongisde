package com.alongside.data.diary

import com.alongside.core.domain.diary.DiaryContentPuller
import com.alongside.core.domain.diary.DiaryEntryRepository
import com.alongside.core.domain.diary.EpisodeRepository
import com.alongside.core.model.diary.DiaryEntry
import com.alongside.core.network.firestore.FirestoreApi
import com.alongside.core.network.firestore.model.CollectionSelector
import com.alongside.core.network.firestore.model.FieldFilter
import com.alongside.core.network.firestore.model.FieldReference
import com.alongside.core.network.firestore.model.FirestoreValue
import com.alongside.core.network.firestore.model.QueryFilter
import com.alongside.core.network.firestore.model.StructuredQuery
import com.alongside.data.episode.EpisodeFirestoreMapper

/**
 * Real [DiaryContentPuller]: queries `diaryEntries`/`episodes` by `tripId`/`diaryEntryId` and
 * writes matches straight into the LOCAL (non-syncing) repositories - using the syncing
 * decorators here would re-enqueue a push of the partner's own document under this device's
 * sync queue, which Firestore rules reject outright (`update` requires `resource.data.userId ==
 * request.auth.uid`).
 */
public class FirestoreDiaryContentPuller(
    private val api: FirestoreApi,
    private val localDiaryEntryRepository: DiaryEntryRepository,
    private val localEpisodeRepository: EpisodeRepository,
) : DiaryContentPuller {
    override suspend fun pullTripContent(
        tripId: String,
        ownUserId: String,
    ) {
        val remoteEntries =
            api
                .runQuery(
                    StructuredQuery(
                        from = listOf(CollectionSelector(DiaryEntryFirestoreMapper.COLLECTION_PATH)),
                        where = QueryFilter(fieldFilter = equalsFilter("tripId", tripId)),
                    ),
                ).map(DiaryEntryFirestoreMapper::fromDocument)

        remoteEntries.forEach { entry ->
            if (entry.userId == ownUserId) {
                pullOwnEntryIfMissingLocally(entry)
            } else {
                upsertEntryIfChanged(entry)
                pullEpisodes(entry.id, onlyIfMissingLocally = false)
            }
        }
    }

    // Own content only ever fills a local gap (docs/roadmap.md M12.7) - never overwrites an
    // existing local row, so a pending local edit still in the sync queue is never clobbered.
    private suspend fun pullOwnEntryIfMissingLocally(entry: DiaryEntry) {
        if (localDiaryEntryRepository.getById(entry.id) != null) return
        localDiaryEntryRepository.upsert(entry)
        pullEpisodes(entry.id, onlyIfMissingLocally = true)
    }

    // Partner content is safe to overwrite unconditionally (local storage never independently
    // edits it) - but re-writing the SAME value on every poll tick still touches Room, which
    // triggers Flow invalidation for every observer regardless of whether the row's content
    // actually changed. On a live screen, that's a full UI recomposition every 5 seconds for
    // nothing - skip the write entirely when the fetched value already matches what's local.
    private suspend fun upsertEntryIfChanged(entry: DiaryEntry) {
        if (localDiaryEntryRepository.getById(entry.id) == entry) return
        localDiaryEntryRepository.upsert(entry)
    }

    private suspend fun pullEpisodes(
        diaryEntryId: String,
        onlyIfMissingLocally: Boolean,
    ) {
        val remoteEpisodes =
            api
                .runQuery(
                    StructuredQuery(
                        from = listOf(CollectionSelector(EpisodeFirestoreMapper.COLLECTION_PATH)),
                        where = QueryFilter(fieldFilter = equalsFilter("diaryEntryId", diaryEntryId)),
                    ),
                ).map(EpisodeFirestoreMapper::fromDocument)

        for (episode in remoteEpisodes) {
            val existing = localEpisodeRepository.getById(episode.id)
            if (onlyIfMissingLocally) {
                if (existing == null) localEpisodeRepository.upsert(episode)
            } else {
                if (existing != episode) localEpisodeRepository.upsert(episode)
            }
        }
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
