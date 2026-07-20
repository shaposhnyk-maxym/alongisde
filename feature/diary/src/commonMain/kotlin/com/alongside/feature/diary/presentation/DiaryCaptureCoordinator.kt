package com.alongside.feature.diary.presentation

import com.alongside.core.domain.diary.DiaryEntryRepository
import com.alongside.core.domain.diary.EpisodeRepository
import com.alongside.core.domain.diary.processing.EpisodeProcessingPipeline
import com.alongside.core.model.SyncStatus
import com.alongside.core.model.diary.DiaryEntry
import com.alongside.core.model.diary.Episode
import com.alongside.feature.diary.capture.ExifPhotoReader
import kotlinx.datetime.LocalDate
import kotlin.time.Clock

private const val DEFAULT_LANGUAGE_TAG = "en"

// Caps only description retries - a still-missing description means Gemini itself is the
// blocker (quota, persistent error), worth giving up on eventually. A still-missing photo
// remoteUrl just means "upload hasn't succeeded yet"; retrying that is cheap/safe and isn't
// capped by this - there's no separate per-photo attempt counter, nor a need for one.
private const val MAX_DESCRIPTION_ATTEMPTS = 5

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

    public suspend fun capture(
        tripId: String,
        userId: String,
        date: LocalDate,
        existingEntryId: String?,
        uris: List<String>,
    ) {
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
        processingPipeline.process(
            diaryEntryId = entryId,
            photos = photos,
            languageTag = DEFAULT_LANGUAGE_TAG,
            // Each cluster's episode is persisted the moment it's built, not batched behind the
            // full photo set - so a later cluster failing can no longer orphan an earlier
            // cluster's already-uploaded-to-Storage photos with nothing local to show for it.
            onEpisodeReady = { episode -> episodeRepository.upsert(episode) },
        )
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

    private fun needsRetry(episode: Episode): Boolean =
        episode.photos.any { it.remoteUrl == null } ||
            (episode.description == null && episode.descriptionAttempts < MAX_DESCRIPTION_ATTEMPTS)
}
