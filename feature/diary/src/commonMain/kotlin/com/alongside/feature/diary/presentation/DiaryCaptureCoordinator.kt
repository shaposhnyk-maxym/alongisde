package com.alongside.feature.diary.presentation

import com.alongside.core.domain.diary.DiaryEntryRepository
import com.alongside.core.domain.diary.EpisodeRepository
import com.alongside.core.domain.diary.processing.EpisodeProcessingPipeline
import com.alongside.core.domain.pairing.PairingRepository
import com.alongside.core.domain.work.BackgroundJobKind
import com.alongside.core.domain.work.BackgroundWorkScheduler
import com.alongside.core.model.SyncStatus
import com.alongside.core.model.diary.DiaryEntry
import com.alongside.core.model.diary.Episode
import com.alongside.feature.diary.capture.ExifPhotoReader
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

private const val DEFAULT_LANGUAGE_TAG = "en"

// Caps only description retries - a still-missing description means Gemini itself is the
// blocker (quota, persistent error), worth giving up on eventually. A still-missing photo
// remoteUrl just means "upload hasn't succeeded yet"; retrying that is cheap/safe and isn't
// capped by this - there's no separate per-photo attempt counter, nor a need for one.
private const val MAX_DESCRIPTION_ATTEMPTS = 5

// Same rationale as MAX_DESCRIPTION_ATTEMPTS, for reverse-geocoding: a still-missing placeName
// means the geocoding client itself is the blocker, worth giving up on eventually rather than
// retrying forever.
private const val MAX_GEOCODE_ATTEMPTS = 5

/**
 * The Timeline's capture write-path: EXIF read -> M10's processing pipeline -> persistence.
 * Split out from [DiaryTimelineContainer] purely to keep its constructor under detekt's
 * `LongParameterList` threshold - the reactive read side ([DiaryEntryRepository]/
 * [EpisodeRepository] observation) stays on the Container since it owns that Flow.
 */
public class DiaryCaptureCoordinator(
    private val diaryEntryRepository: DiaryEntryRepository,
    private val episodeRepository: EpisodeRepository,
    private val processingPipeline: EpisodeProcessingPipeline,
    private val exifPhotoReader: ExifPhotoReader,
    private val pairingRepository: PairingRepository,
    private val backgroundWorkScheduler: BackgroundWorkScheduler,
    private val clock: Clock = Clock.System,
) {
    // Deterministic, not Uuid.random() - two racy captures for a day with no entry yet (Orbit
    // intents run concurrently; the reactive state.ownEntries read can also just be stale) must
    // land on the SAME id regardless of timing, or buildDiaryTimelineDay's associateBy silently
    // drops one of the two resulting entries (and its episodes) from display for good.
    private fun deterministicNewEntryId(
        tripId: String,
        userId: String,
        date: LocalDate,
    ): String = "$tripId::$userId::$date"

    /**
     * Defensive, not the primary guard - the Timeline UI already hides "Add Photos" once a day's
     * date has passed (docs/roadmap.md M12.12: `MISSED` is computed, not stored, so a backdated
     * capture would just flip a missed day back to `READY`). This is belt-and-suspenders for any
     * other entry point that might one day reach `capture()` without going through that UI gate.
     */
    public suspend fun capture(
        tripId: String,
        userId: String,
        date: LocalDate,
        existingEntryId: String?,
        uris: List<String>,
    ) {
        val today = clock.todayIn(TimeZone.currentSystemDefault())
        if (date < today) {
            println("DiaryCaptureCoordinator: rejected capture for a past day ($date < $today)")
            return
        }

        val entryId = existingEntryId ?: deterministicNewEntryId(tripId, userId, date)

        // Persisted up front, before any processing - a photo-read or per-cluster failure below
        // must never cost the day its DiaryEntry, only the affected episode(s).
        diaryEntryRepository.upsert(
            DiaryEntry(
                id = entryId,
                tripId = tripId,
                userId = userId,
                date = date,
                syncStatus = SyncStatus.PENDING,
                createdAt = clock.now(),
                updatedAt = clock.now(),
            ),
        )

        val photos = exifPhotoReader.readExifPhotos(uris)
        val builtEpisodes = mutableListOf<Episode>()
        processingPipeline.process(
            diaryEntryId = entryId,
            photos = photos,
            languageTag = DEFAULT_LANGUAGE_TAG,
            // Each cluster's episode is persisted the moment it's built, not batched behind the
            // full photo set - so a later cluster failing can no longer orphan an earlier
            // cluster's already-uploaded-to-Storage photos with nothing local to show for it.
            onEpisodeReady = { episode ->
                episodeRepository.upsert(episode)
                builtEpisodes += episode
            },
        )
        // Event-driven enqueue (docs/roadmap.md M12.11) - the periodic sweep is only a backstop
        // for a missed enqueue, not the primary path.
        if (builtEpisodes.any(::needsRetry)) {
            backgroundWorkScheduler.scheduleOneOff(BackgroundJobKind.EPISODE_RETRY)
        }
    }

    /** Final - once closed, [entry]'s date can never receive an existing-entry match again. */
    public suspend fun closeDay(entry: DiaryEntry) {
        diaryEntryRepository.upsert(entry.copy(closedAt = clock.now()))
    }

    /**
     * Background maintenance pass (see `DiaryTimelineDataSource`'s poll loop): a capture-time
     * hiccup (no network) can leave an episode with a photo missing its `remoteUrl` (only ever
     * visible locally, never synced - the partner's device can't load a bare local URI) and/or a
     * null `description` - neither self-heals on its own, since nothing else ever revisits an
     * already-persisted episode. Only [episodes] actually needing it get retried.
     */
    public suspend fun retryIncompleteEpisodes(episodes: List<Episode>) {
        for (episode in episodes) {
            if (!needsRetry(episode)) continue
            val retried = processingPipeline.retryIncomplete(episode, DEFAULT_LANGUAGE_TAG)
            if (retried != episode) episodeRepository.upsert(retried)
        }
    }

    /**
     * Worker-facing entry point (docs/roadmap.md M12.11): unlike [retryIncompleteEpisodes], which
     * needs an already-gathered episode list (the Timeline Container has one for free in its
     * reactive state), this gathers [ownUserId]'s own incomplete episodes from scratch - there's
     * no Container/reactive state to read from a background Worker. Mirrors the same trip/entry
     * lookups the Timeline itself uses.
     */
    public suspend fun retryAllIncompleteEpisodes(ownUserId: String) {
        val trip = pairingRepository.getActiveTrip(ownUserId) ?: return
        val ownEntries = diaryEntryRepository.observeByTrip(trip.id).first().filter { it.userId == ownUserId }
        val episodes = ownEntries.flatMap { entry -> episodeRepository.observeByDiaryEntry(entry.id).first() }
        retryIncompleteEpisodes(episodes)
    }

    private fun needsRetry(episode: Episode): Boolean =
        episode.photos.any { it.remoteUrl == null } ||
            (episode.description == null && episode.descriptionAttempts < MAX_DESCRIPTION_ATTEMPTS) ||
            (episode.placeName == null && episode.geocodeAttempts < MAX_GEOCODE_ATTEMPTS)
}
