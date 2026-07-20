package com.alongside.feature.diary.presentation

import com.alongside.core.domain.diary.processing.EpisodeProcessingPipeline
import com.alongside.core.model.SyncStatus
import com.alongside.core.model.diary.Episode
import com.alongside.core.model.diary.Photo
import com.alongside.feature.diary.FakeDiaryEntryRepository
import com.alongside.feature.diary.FakeEpisodeRepository
import com.alongside.feature.diary.FakeExifPhotoReader
import com.alongside.feature.diary.FakeGeocodingClient
import com.alongside.feature.diary.FakePhotoUploadClient
import com.alongside.feature.diary.FakeVisionClient
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Instant

private val CAPTURE_FIXED_NOW = Instant.parse("2026-07-19T12:00:00Z")

private object CaptureFixedClock : Clock {
    override fun now(): Instant = CAPTURE_FIXED_NOW
}

class DiaryCaptureCoordinatorTest {
    private val diaryEntryRepository = FakeDiaryEntryRepository()
    private val episodeRepository = FakeEpisodeRepository()

    private fun coordinator(exifPhotoReader: FakeExifPhotoReader) =
        DiaryCaptureCoordinator(
            diaryEntryRepository = diaryEntryRepository,
            episodeRepository = episodeRepository,
            processingPipeline =
                EpisodeProcessingPipeline(
                    geocodingClient = FakeGeocodingClient(),
                    visionDescriptionClient = FakeVisionClient(),
                    imageBytesLoader = { byteArrayOf(1) },
                    photoUploadClient = FakePhotoUploadClient(),
                    clock = CaptureFixedClock,
                ),
            exifPhotoReader = exifPhotoReader,
            clock = CaptureFixedClock,
        )

    private fun photo(
        id: String,
        remoteUrl: String? = null,
    ) = Photo(id = id, uri = "content://$id", takenAt = CAPTURE_FIXED_NOW, latitude = 1.0, longitude = 1.0, remoteUrl = remoteUrl)

    private fun incompleteEpisode(
        photos: List<Photo>,
        description: String? = null,
        descriptionAttempts: Int = 1,
    ) = Episode(
        id = "episode-1",
        diaryEntryId = "entry-1",
        startTime = CAPTURE_FIXED_NOW,
        endTime = CAPTURE_FIXED_NOW,
        latitude = 1.0,
        longitude = 1.0,
        placeName = "Rynok Square",
        description = description,
        descriptionAttempts = descriptionAttempts,
        photos = photos,
        syncStatus = SyncStatus.PENDING,
        updatedAt = CAPTURE_FIXED_NOW,
    )

    // The exact race this guards against: Orbit intents run concurrently and the reactive
    // state.ownEntries read backing existingEntryId can be stale (see DiaryCaptureCoordinator's
    // KDoc) - two back-to-back captures for a day with no entry yet must never mint two
    // different ids, or buildDiaryTimelineDay's associateBy silently drops one from display.
    @Test
    fun `two captures for the same day with no existing entry land on the same deterministic id`() =
        runTest {
            val date = LocalDate(2026, 7, 19)
            val underTest = coordinator(FakeExifPhotoReader(mapOf("content://p1" to photo("p1"), "content://p2" to photo("p2"))))

            underTest.capture(
                tripId = "trip-1",
                userId = "user-1",
                date = date,
                existingEntryId = null,
                uris = listOf("content://p1"),
            )
            underTest.capture(
                tripId = "trip-1",
                userId = "user-1",
                date = date,
                existingEntryId = null,
                uris = listOf("content://p2"),
            )

            val entryIds = diaryEntryRepository.upserted.map { it.id }.toSet()
            assertEquals(1, entryIds.size)
            val entryId = entryIds.single()
            assertEquals(2, episodeRepository.upserted.size)
            assertEquals(setOf(entryId), episodeRepository.upserted.map { it.diaryEntryId }.toSet())
        }

    @Test
    fun `a capture with an existing entry id reuses it instead of the deterministic fallback`() =
        runTest {
            val date = LocalDate(2026, 7, 19)
            val underTest = coordinator(FakeExifPhotoReader(mapOf("content://p1" to photo("p1"))))

            underTest.capture(
                tripId = "trip-1",
                userId = "user-1",
                date = date,
                existingEntryId = "pre-existing-entry-id",
                uris = listOf("content://p1"),
            )

            assertEquals("pre-existing-entry-id", diaryEntryRepository.upserted.single().id)
            assertEquals("pre-existing-entry-id", episodeRepository.upserted.single().diaryEntryId)
        }

    @Test
    fun `the diary entry is persisted even before any episode is built`() =
        runTest {
            val date = LocalDate(2026, 7, 19)
            val underTest = coordinator(FakeExifPhotoReader(emptyMap()))

            underTest.capture(
                tripId = "trip-1",
                userId = "user-1",
                date = date,
                existingEntryId = null,
                uris = listOf("content://unknown"),
            )

            assertEquals(1, diaryEntryRepository.upserted.size)
            assertEquals(0, episodeRepository.upserted.size)
        }

    @Test
    fun `retryIncompleteEpisodes heals a photo missing its remoteUrl and a missing description`() =
        runTest {
            val underTest = coordinator(FakeExifPhotoReader(emptyMap()))
            val episode = incompleteEpisode(photos = listOf(photo("p1")))

            underTest.retryIncompleteEpisodes(listOf(episode))

            val healed = episodeRepository.upserted.single()
            assertEquals("https://storage/p1", healed.photos.single().remoteUrl)
            assertEquals("A wander through the old town.", healed.description)
        }

    @Test
    fun `retryIncompleteEpisodes leaves an already-complete episode alone`() =
        runTest {
            val underTest = coordinator(FakeExifPhotoReader(emptyMap()))
            val episode =
                incompleteEpisode(
                    photos = listOf(photo("p1", remoteUrl = "https://storage/p1")),
                    description = "already generated",
                )

            underTest.retryIncompleteEpisodes(listOf(episode))

            assertEquals(0, episodeRepository.upserted.size)
        }

    @Test
    fun `retryIncompleteEpisodes gives up on a missing description past the attempt cap`() =
        runTest {
            val underTest = coordinator(FakeExifPhotoReader(emptyMap()))
            val episode =
                incompleteEpisode(
                    photos = listOf(photo("p1", remoteUrl = "https://storage/p1")),
                    description = null,
                    descriptionAttempts = 5,
                )

            underTest.retryIncompleteEpisodes(listOf(episode))

            assertEquals(0, episodeRepository.upserted.size)
        }

    @Test
    fun `retryIncompleteEpisodes still retries a missing photo upload regardless of the description attempt cap`() =
        runTest {
            val underTest = coordinator(FakeExifPhotoReader(emptyMap()))
            val episode =
                incompleteEpisode(
                    photos = listOf(photo("p1")),
                    description = "already generated",
                    descriptionAttempts = 5,
                )

            underTest.retryIncompleteEpisodes(listOf(episode))

            assertEquals(
                "https://storage/p1",
                episodeRepository.upserted
                    .single()
                    .photos
                    .single()
                    .remoteUrl,
            )
        }

    @Test
    fun `retryIncompleteEpisodes with an empty list upserts nothing`() =
        runTest {
            val underTest = coordinator(FakeExifPhotoReader(emptyMap()))

            underTest.retryIncompleteEpisodes(emptyList())

            assertEquals(0, episodeRepository.upserted.size)
        }
}
