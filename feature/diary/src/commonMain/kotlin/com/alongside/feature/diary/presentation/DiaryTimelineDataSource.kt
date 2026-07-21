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

/**
 * Owns the Timeline's reactive read side (trip -> entries -> episodes, local Room only) plus the
 * remote trip-content pull loop (partner updates + own-gap fill, see [DiaryContentPuller]) -
 * split out from [DiaryTimelineContainer] purely to keep its constructor under detekt's
 * LongParameterList threshold, the same reasoning [DiaryCaptureCoordinator] already documents
 * for the write side.
 *
 * Own-episode retry (a capture-time hiccup leaving a photo/description incomplete) used to run
 * as a 30s in-memory poll loop here - tied to this Container's lifetime, so it never ran once the
 * app was backgrounded/killed. That's now WorkManager's job (docs/roadmap.md M12.11,
 * [DiaryCaptureCoordinator.retryAllIncompleteEpisodes]), which survives process death. This class
 * keeps only a one-shot foreground "nudge" (see [nudgeIncompleteEpisodesOnce]) - a cheap UX
 * improvement for someone watching this exact screen, not a reliability mechanism.
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
        var nudged = false

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

            tripFlow
                .flatMapLatest { trip -> observeEntriesAndEpisodes(trip) }
                .collect { snapshot ->
                    onUpdate(snapshot)
                    if (!nudged && snapshot.second.isNotEmpty()) {
                        nudged = true
                        nudgeIncompleteEpisodesOnce(ownUserId, snapshot)
                    }
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

    // A single fire-once nudge (no delay/loop) reusing whatever episode list this screen already
    // has in memory - papers over WorkManager's enqueue not being instant for someone actively
    // watching their photo upload/description appear. Dies with this screen; the WorkManager path
    // (event-driven enqueue + periodic sweep) is what actually guarantees healing happens at all.
    private suspend fun nudgeIncompleteEpisodesOnce(
        ownUserId: String,
        snapshot: EntriesAndEpisodes,
    ) {
        val (_, entries, episodesByEntryId) = snapshot
        val ownEpisodes = entries.filter { it.userId == ownUserId }.flatMap { episodesByEntryId[it.id].orEmpty() }
        if (ownEpisodes.isEmpty()) return
        runCatching { captureCoordinator.retryIncompleteEpisodes(ownEpisodes) }
            .onFailure { println("DiaryTimelineDataSource: retryIncompleteEpisodes nudge failed: $it") }
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
