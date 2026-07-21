package com.alongside.feature.diary.presentation

import com.alongside.core.domain.diary.processing.EpisodeProcessingPipeline
import com.alongside.core.domain.diary.processing.GeocodingResult
import com.alongside.core.domain.diary.processing.PhotoUploadResult
import com.alongside.core.domain.work.BackgroundJobKind
import com.alongside.core.model.SyncStatus
import com.alongside.core.model.diary.Episode
import com.alongside.core.model.diary.Photo
import com.alongside.feature.diary.FakeBackgroundWorkScheduler
import com.alongside.feature.diary.FakeDiaryEntryRepository
import com.alongside.feature.diary.FakeEpisodeRepository
import com.alongside.feature.diary.FakeExifPhotoReader
import com.alongside.feature.diary.FakeGeocodingClient
import com.alongside.feature.diary.FakePairingRepository
import com.alongside.feature.diary.FakePhotoUploadClient
import com.alongside.feature.diary.FakeVisionClient
import com.alongside.feature.diary.fakeTrip
import com.alongside.feature.diary.testDiaryEntry
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
    private val pairingRepository = FakePairingRepository()
    private val backgroundWorkScheduler = FakeBackgroundWorkScheduler()

    private fun coordinator(
        exifPhotoReader: FakeExifPhotoReader,
        photoUploadClient: FakePhotoUploadClient = FakePhotoUploadClient(),
        geocodingClient: FakeGeocodingClient = FakeGeocodingClient(),
    ) = DiaryCaptureCoordinator(
        diaryEntryRepository = diaryEntryRepository,
        episodeRepository = episodeRepository,
        processingPipeline =
            EpisodeProcessingPipeline(
                geocodingClient = geocodingClient,
                visionDescriptionClient = FakeVisionClient(),
                imageBytesLoader = { byteArrayOf(1) },
                photoUploadClient = photoUploadClient,
                clock = CaptureFixedClock,
            ),
        exifPhotoReader = exifPhotoReader,
        pairingRepository = pairingRepository,
        backgroundWorkScheduler = backgroundWorkScheduler,
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
        id: String = "episode-1",
        diaryEntryId: String = "entry-1",
        placeName: String? = "Rynok Square",
        geocodeAttempts: Int = 1,
    ) = Episode(
        id = id,
        diaryEntryId = diaryEntryId,
        startTime = CAPTURE_FIXED_NOW,
        endTime = CAPTURE_FIXED_NOW,
        latitude = 1.0,
        longitude = 1.0,
        placeName = placeName,
        description = description,
        descriptionAttempts = descriptionAttempts,
        photos = photos,
        syncStatus = SyncStatus.PENDING,
        updatedAt = CAPTURE_FIXED_NOW,
        geocodeAttempts = geocodeAttempts,
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
    fun `retryIncompleteEpisodes re-geocodes a missing placeName`() =
        runTest {
            val underTest = coordinator(FakeExifPhotoReader(emptyMap()))
            val episode =
                incompleteEpisode(
                    photos = listOf(photo("p1", remoteUrl = "https://storage/p1")),
                    description = "already generated",
                    placeName = null,
                    geocodeAttempts = 1,
                )

            underTest.retryIncompleteEpisodes(listOf(episode))

            assertEquals("Rynok Square", episodeRepository.upserted.single().placeName)
        }

    @Test
    fun `retryIncompleteEpisodes gives up on a missing placeName past the attempt cap`() =
        runTest {
            val underTest = coordinator(FakeExifPhotoReader(emptyMap()))
            val episode =
                incompleteEpisode(
                    photos = listOf(photo("p1", remoteUrl = "https://storage/p1")),
                    description = "already generated",
                    placeName = null,
                    geocodeAttempts = 5,
                )

            underTest.retryIncompleteEpisodes(listOf(episode))

            assertEquals(0, episodeRepository.upserted.size)
        }

    @Test
    fun `capture enqueues EPISODE_RETRY when the resulting episode still needs geocoding retry`() =
        runTest {
            val underTest =
                coordinator(
                    FakeExifPhotoReader(mapOf("content://p1" to photo("p1"))),
                    geocodingClient = FakeGeocodingClient(result = GeocodingResult.NotFound),
                )

            underTest.capture(
                tripId = "trip-1",
                userId = "user-1",
                date = LocalDate(2026, 7, 19),
                existingEntryId = null,
                uris = listOf("content://p1"),
            )

            assertEquals(listOf(BackgroundJobKind.EPISODE_RETRY), backgroundWorkScheduler.scheduledOneOffs)
        }

    @Test
    fun `retryIncompleteEpisodes with an empty list upserts nothing`() =
        runTest {
            val underTest = coordinator(FakeExifPhotoReader(emptyMap()))

            underTest.retryIncompleteEpisodes(emptyList())

            assertEquals(0, episodeRepository.upserted.size)
        }

    @Test
    fun `capture enqueues EPISODE_RETRY when the resulting episode still needs retry`() =
        runTest {
            val underTest =
                coordinator(
                    FakeExifPhotoReader(mapOf("content://p1" to photo("p1"))),
                    photoUploadClient = FakePhotoUploadClient { PhotoUploadResult.Failure(RuntimeException("offline")) },
                )

            underTest.capture(
                tripId = "trip-1",
                userId = "user-1",
                date = LocalDate(2026, 7, 19),
                existingEntryId = null,
                uris = listOf("content://p1"),
            )

            assertEquals(listOf(BackgroundJobKind.EPISODE_RETRY), backgroundWorkScheduler.scheduledOneOffs)
        }

    @Test
    fun `capture does not enqueue EPISODE_RETRY when the resulting episode is already complete`() =
        runTest {
            val underTest = coordinator(FakeExifPhotoReader(mapOf("content://p1" to photo("p1"))))

            underTest.capture(
                tripId = "trip-1",
                userId = "user-1",
                date = LocalDate(2026, 7, 19),
                existingEntryId = null,
                uris = listOf("content://p1"),
            )

            assertEquals(emptyList(), backgroundWorkScheduler.scheduledOneOffs)
        }

    @Test
    fun `retryAllIncompleteEpisodes retries only own incomplete episodes under the attempt cap`() =
        runTest {
            val underTest = coordinator(FakeExifPhotoReader(emptyMap()))
            pairingRepository.activeTrip.value = fakeTrip(id = "trip-1")
            diaryEntryRepository.upsert(testDiaryEntry(id = "own-entry", tripId = "trip-1", userId = "user-1"))
            diaryEntryRepository.upsert(testDiaryEntry(id = "partner-entry", tripId = "trip-1", userId = "partner-1"))
            episodeRepository.upsert(
                incompleteEpisode(id = "own-incomplete", diaryEntryId = "own-entry", photos = listOf(photo("p1"))),
            )
            episodeRepository.upsert(
                incompleteEpisode(
                    id = "own-complete",
                    diaryEntryId = "own-entry",
                    photos = listOf(photo("p2", remoteUrl = "https://storage/p2")),
                    description = "already generated",
                ),
            )
            episodeRepository.upsert(
                incompleteEpisode(
                    id = "own-capped",
                    diaryEntryId = "own-entry",
                    photos = listOf(photo("p3", remoteUrl = "https://storage/p3")),
                    description = null,
                    descriptionAttempts = 5,
                ),
            )
            episodeRepository.upsert(
                incompleteEpisode(id = "partner-incomplete", diaryEntryId = "partner-entry", photos = listOf(photo("p4"))),
            )
            episodeRepository.upserted.clear()

            underTest.retryAllIncompleteEpisodes("user-1")

            assertEquals(listOf("own-incomplete"), episodeRepository.upserted.map { it.id })
        }

    @Test
    fun `retryAllIncompleteEpisodes is a no-op when there is no active trip`() =
        runTest {
            val underTest = coordinator(FakeExifPhotoReader(emptyMap()))

            underTest.retryAllIncompleteEpisodes("user-1")

            assertEquals(0, episodeRepository.upserted.size)
        }
}
