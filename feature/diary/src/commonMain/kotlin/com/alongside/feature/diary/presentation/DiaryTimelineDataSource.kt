package com.alongside.feature.diary.presentation

import com.alongside.core.domain.diary.DiaryContentPuller
import com.alongside.core.domain.diary.DiaryEntryRepository
import com.alongside.core.domain.diary.EpisodeRepository
import com.alongside.core.domain.pairing.PairingRepository
import com.alongside.core.model.diary.DiaryEntry
import com.alongside.core.model.diary.Episode
import com.alongside.core.model.trip.Trip
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

public typealias EntriesAndEpisodes = Triple<Trip?, List<DiaryEntry>, Map<String, List<Episode>>>

private val TRIP_CONTENT_POLL_INTERVAL = 5.seconds

// Deliberately much less frequent than TRIP_CONTENT_POLL_INTERVAL - this drives real Gemini
// vision/Storage upload calls, not a cheap local Firestore query.
private val INCOMPLETE_EPISODE_RETRY_POLL_INTERVAL = 30.seconds

/**
 * Owns the Timeline's reactive read side (trip -> entries -> episodes, local Room only) plus the
 * remote trip-content pull loop (partner updates + own-gap fill, see [DiaryContentPuller]) and a
 * background retry loop for own episodes a capture-time hiccup left incomplete (see
 * [DiaryCaptureCoordinator.retryIncompleteEpisodes]) - split out from [DiaryTimelineContainer]
 * purely to keep its constructor under detekt's LongParameterList threshold, the same reasoning
 * [DiaryCaptureCoordinator] already documents for the write side.
 */
public class DiaryTimelineDataSource(
    private val pairingRepository: PairingRepository,
    private val diaryEntryRepository: DiaryEntryRepository,
    private val episodeRepository: EpisodeRepository,
    private val diaryContentPuller: DiaryContentPuller,
    private val captureCoordinator: DiaryCaptureCoordinator,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    public suspend fun observe(
        ownUserId: String,
        onUpdate: suspend (EntriesAndEpisodes) -> Unit,
    ) {
        val tripFlow = pairingRepository.observeActiveTrip(ownUserId)
        val latest = MutableStateFlow<EntriesAndEpisodes?>(null)

        coroutineScope {
            // A second, independent collection of tripFlow - accepted duplicate polling of the
            // underlying pairing poll loop (see FirestorePairingTripDataSource) in exchange for
            // clean cancellation: distinctUntilChanged + collectLatest means exactly one trip-
            // content poll loop runs at a time, automatically replaced (not stacked) when the
            // active trip changes, and stopped entirely when it goes null.
            launch {
                tripFlow
                    .map { it?.id }
                    .distinctUntilChanged()
                    .collectLatest { tripId -> if (tripId != null) pollTripContent(tripId, ownUserId) }
            }

            launch { pollIncompleteEpisodes(ownUserId, latest) }

            tripFlow
                .flatMapLatest { trip -> observeEntriesAndEpisodes(trip) }
                .collect {
                    latest.value = it
                    onUpdate(it)
                }
        }
    }

    private suspend fun pollTripContent(
        tripId: String,
        ownUserId: String,
    ) {
        while (true) {
            runCatching { diaryContentPuller.pullTripContent(tripId, ownUserId) }
                .onFailure { println("DiaryTimelineDataSource: pullTripContent failed: $it") }
            delay(TRIP_CONTENT_POLL_INTERVAL)
        }
    }

    private suspend fun pollIncompleteEpisodes(
        ownUserId: String,
        latest: MutableStateFlow<EntriesAndEpisodes?>,
    ) {
        while (true) {
            delay(INCOMPLETE_EPISODE_RETRY_POLL_INTERVAL)
            val (_, entries, episodesByEntryId) = latest.value ?: continue
            val ownEpisodes = entries.filter { it.userId == ownUserId }.flatMap { episodesByEntryId[it.id].orEmpty() }
            runCatching { captureCoordinator.retryIncompleteEpisodes(ownEpisodes) }
                .onFailure { println("DiaryTimelineDataSource: retryIncompleteEpisodes failed: $it") }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeEntriesAndEpisodes(trip: Trip?): Flow<EntriesAndEpisodes> {
        if (trip == null) return flowOf(Triple(null, emptyList(), emptyMap()))
        return diaryEntryRepository.observeByTrip(trip.id).flatMapLatest { entries ->
            if (entries.isEmpty()) {
                flowOf(Triple(trip, entries, emptyMap()))
            } else {
                combine(
                    entries.map { entry -> episodeRepository.observeByDiaryEntry(entry.id) },
                ) { episodeLists ->
                    val byEntryId = entries.indices.associate { index -> entries[index].id to episodeLists[index] }
                    Triple(trip, entries, byEntryId)
                }
            }
        }
    }
}
