package com.alongside.core.domain.diary.processing

import com.alongside.core.model.SyncStatus
import com.alongside.core.model.diary.Episode
import com.alongside.core.model.diary.Photo
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * Fake [PlaceGeocodingClient] - scriptable per call, records the queried coordinates.
 * [throwOnCallNumber] (1-based) simulates an uncaught exception escaping a real client
 * implementation, distinct from [GeocodingResult.Failure] which every real client already
 * converts every exception into (docs/roadmap.md's capture-pipeline-resilience fix).
 */
private class FakePlaceGeocodingClient(
    private val result: GeocodingResult = GeocodingResult.Found("Rynok Square"),
    private val throwOnCallNumber: Int? = null,
) : PlaceGeocodingClient {
    val queries = mutableListOf<Pair<Double, Double>>()

    override suspend fun reverseGeocode(
        latitude: Double,
        longitude: Double,
    ): GeocodingResult {
        queries += latitude to longitude
        if (queries.size == throwOnCallNumber) error("simulated uncaught failure")
        return result
    }
}

/** Fake [PhotoUploadClient] - records every photo it was asked to upload, scriptable per photo. */
private class FakePhotoUploadClient(
    private val resultFor: (Photo) -> PhotoUploadResult = { PhotoUploadResult.Uploaded("https://storage/${it.id}") },
) : PhotoUploadClient {
    val uploaded = mutableListOf<Photo>()

    override suspend fun upload(
        photo: Photo,
        bytes: ByteArray,
    ): PhotoUploadResult {
        uploaded += photo
        return resultFor(photo)
    }
}

/** Fake [EpisodeVisionDescriptionClient] - scriptable, records what it was called with. */
private class FakeEpisodeVisionDescriptionClient(
    private val result: VisionDescriptionResult = VisionDescriptionResult.Generated("A wander through the old town."),
) : EpisodeVisionDescriptionClient {
    var lastImageCount: Int? = null
    var lastPlaceName: String? = null
    var lastLanguageTag: String? = null

    override suspend fun describeEpisode(
        images: List<ByteArray>,
        placeName: String?,
        languageTag: String,
    ): VisionDescriptionResult {
        lastImageCount = images.size
        lastPlaceName = placeName
        lastLanguageTag = languageTag
        return result
    }
}

private val FIXED_NOW = Instant.fromEpochMilliseconds(1_752_700_000_000)

private object FixedClock : Clock {
    override fun now(): Instant = FIXED_NOW
}

class EpisodeProcessingPipelineTest {
    private val baseTime = Instant.fromEpochMilliseconds(1_752_600_000_000)

    private fun photo(
        id: String,
        offsetMinutes: Int,
    ) = Photo(
        id = id,
        uri = "content://photos/$id",
        takenAt = baseTime + offsetMinutes.minutes,
        latitude = 49.8397,
        longitude = 24.0297,
    )

    @Test
    fun `processes one cluster into one episode with geocoded name and generated description`() =
        runTest {
            val geocoding = FakePlaceGeocodingClient()
            val vision = FakeEpisodeVisionDescriptionClient()
            val pipeline =
                EpisodeProcessingPipeline(
                    geocodingClient = geocoding,
                    visionDescriptionClient = vision,
                    imageBytesLoader = { byteArrayOf(1) },
                    photoUploadClient = FakePhotoUploadClient(),
                    generateEpisodeId = { "episode-1" },
                    clock = FixedClock,
                )
            val photos = listOf(photo("p1", 0), photo("p2", 10))

            val episodes = pipeline.process(diaryEntryId = "entry-1", photos = photos, languageTag = "en")

            assertEquals(1, episodes.size)
            val episode = episodes.single()
            assertEquals("episode-1", episode.id)
            assertEquals("entry-1", episode.diaryEntryId)
            assertEquals("Rynok Square", episode.placeName)
            assertEquals("A wander through the old town.", episode.description)
            assertEquals(1, episode.descriptionAttempts)
            assertEquals(photos.map { it.id }, episode.photos.map { it.id })
            assertEquals(1, geocoding.queries.size)
            assertEquals(SyncStatus.PENDING, episode.syncStatus)
            assertEquals(FIXED_NOW, episode.updatedAt)
        }

    @Test
    fun `geocoded city and cityPlaceId and countryCode are carried onto the episode`() =
        runTest {
            val geocoding =
                FakePlaceGeocodingClient(
                    result =
                        GeocodingResult.Found(
                            placeName = "Rynok Square",
                            city = "Lviv",
                            cityPlaceId = "locality-place-id",
                            countryCode = "UA",
                        ),
                )
            val pipeline =
                EpisodeProcessingPipeline(
                    geocodingClient = geocoding,
                    visionDescriptionClient = FakeEpisodeVisionDescriptionClient(),
                    imageBytesLoader = { byteArrayOf(1) },
                    photoUploadClient = FakePhotoUploadClient(),
                    generateEpisodeId = { "episode-1" },
                    clock = FixedClock,
                )
            val photos = listOf(photo("p1", 0), photo("p2", 10))

            val episode = pipeline.process(diaryEntryId = "entry-1", photos = photos, languageTag = "en").single()

            assertEquals("Lviv", episode.city)
            assertEquals("locality-place-id", episode.cityPlaceId)
            assertEquals("UA", episode.countryCode)
        }

    @Test
    fun `processes multiple clusters into multiple episodes`() =
        runTest {
            val ids = listOf("episode-1", "episode-2").iterator()
            val pipeline =
                EpisodeProcessingPipeline(
                    geocodingClient = FakePlaceGeocodingClient(),
                    visionDescriptionClient = FakeEpisodeVisionDescriptionClient(),
                    imageBytesLoader = { byteArrayOf(1) },
                    photoUploadClient = FakePhotoUploadClient(),
                    generateEpisodeId = { ids.next() },
                )
            val photos = listOf(photo("p1", 0), photo("p2", 300))

            val episodes = pipeline.process(diaryEntryId = "entry-1", photos = photos, languageTag = "en")

            assertEquals(2, episodes.size)
        }

    @Test
    fun `onEpisodeReady fires per cluster as each one completes in order`() =
        runTest {
            val ready = mutableListOf<String>()
            val ids = listOf("episode-1", "episode-2").iterator()
            val pipeline =
                EpisodeProcessingPipeline(
                    geocodingClient = FakePlaceGeocodingClient(),
                    visionDescriptionClient = FakeEpisodeVisionDescriptionClient(),
                    imageBytesLoader = { byteArrayOf(1) },
                    photoUploadClient = FakePhotoUploadClient(),
                    generateEpisodeId = { ids.next() },
                )
            val photos = listOf(photo("p1", 0), photo("p2", 300))

            pipeline.process("entry-1", photos, "en", onEpisodeReady = { ready += it.id })

            assertEquals(listOf("episode-1", "episode-2"), ready)
        }

    @Test
    fun `a cluster that throws an uncaught exception is skipped not fatal to the whole batch`() =
        runTest {
            val ready = mutableListOf<Set<String>>()
            val uploadClient = FakePhotoUploadClient()
            val pipeline =
                EpisodeProcessingPipeline(
                    // 2nd reverseGeocode call = the 2nd cluster (3 clusters, 1 geocode call each).
                    geocodingClient = FakePlaceGeocodingClient(throwOnCallNumber = 2),
                    visionDescriptionClient = FakeEpisodeVisionDescriptionClient(),
                    imageBytesLoader = { byteArrayOf(1) },
                    photoUploadClient = uploadClient,
                    generateEpisodeId = { "episode-${ready.size + 1}" },
                )
            val photos = listOf(photo("p1", 0), photo("p2", 300), photo("p3", 600))

            val episodes =
                pipeline.process("entry-1", photos, "en", onEpisodeReady = { ready += it.photos.map { p -> p.id }.toSet() })

            // Cluster 2 (p2) failed and is gone entirely, but clusters 1 (p1) and 3 (p3) -
            // already processed independently, including their own Storage upload - are neither
            // lost nor blocked by it: only p1/p3 ever reached the upload client.
            assertEquals(listOf(setOf("p1"), setOf("p3")), episodes.map { it.photos.map { p -> p.id }.toSet() })
            assertEquals(listOf(setOf("p1"), setOf("p3")), ready)
            assertEquals(setOf("p1", "p3"), uploadClient.uploaded.map { it.id }.toSet())
        }

    @Test
    fun `a photo whose bytes fail to load degrades that photo instead of throwing`() =
        runTest {
            val pipeline =
                EpisodeProcessingPipeline(
                    geocodingClient = FakePlaceGeocodingClient(),
                    visionDescriptionClient = FakeEpisodeVisionDescriptionClient(),
                    imageBytesLoader = { photo -> if (photo.id == "p2") error("simulated read failure") else byteArrayOf(1) },
                    photoUploadClient = FakePhotoUploadClient(),
                    generateEpisodeId = { "episode-1" },
                )
            val photos = listOf(photo("p1", 0), photo("p2", 10), photo("p3", 20))

            val episode = pipeline.process("entry-1", photos, "en").single()

            assertEquals("https://storage/p1", episode.photos.single { it.id == "p1" }.remoteUrl)
            assertNull(episode.photos.single { it.id == "p2" }.remoteUrl)
            assertEquals("https://storage/p3", episode.photos.single { it.id == "p3" }.remoteUrl)
        }

    @Test
    fun `geocoding NotFound leaves placeName null but still generates a description`() =
        runTest {
            val pipeline =
                EpisodeProcessingPipeline(
                    geocodingClient = FakePlaceGeocodingClient(result = GeocodingResult.NotFound),
                    visionDescriptionClient = FakeEpisodeVisionDescriptionClient(),
                    imageBytesLoader = { byteArrayOf(1) },
                    photoUploadClient = FakePhotoUploadClient(),
                    generateEpisodeId = { "episode-1" },
                )

            val episode = pipeline.process("entry-1", listOf(photo("p1", 0)), "en").single()

            assertNull(episode.placeName)
            assertEquals("A wander through the old town.", episode.description)
        }

    @Test
    fun `vision failure leaves description null but still counts as an attempt`() =
        runTest {
            val pipeline =
                EpisodeProcessingPipeline(
                    geocodingClient = FakePlaceGeocodingClient(),
                    visionDescriptionClient =
                        FakeEpisodeVisionDescriptionClient(result = VisionDescriptionResult.Failure(RuntimeException("boom"))),
                    imageBytesLoader = { byteArrayOf(1) },
                    photoUploadClient = FakePhotoUploadClient(),
                    generateEpisodeId = { "episode-1" },
                )

            val episode = pipeline.process("entry-1", listOf(photo("p1", 0)), "en").single()

            assertNull(episode.description)
            assertEquals(1, episode.descriptionAttempts)
        }

    @Test
    fun `feeds only the representative photo subset to the vision client`() =
        runTest {
            val vision = FakeEpisodeVisionDescriptionClient()
            val pipeline =
                EpisodeProcessingPipeline(
                    geocodingClient = FakePlaceGeocodingClient(),
                    visionDescriptionClient = vision,
                    imageBytesLoader = { byteArrayOf(1) },
                    photoUploadClient = FakePhotoUploadClient(),
                    generateEpisodeId = { "episode-1" },
                )
            // 6 photos in one cluster - representative selection caps at MAX_REPRESENTATIVE_PHOTOS (4).
            val photos = (0..5).map { photo("p$it", it) }

            val episode = pipeline.process("entry-1", photos, "uk").single()

            assertEquals(MAX_REPRESENTATIVE_PHOTOS, vision.lastImageCount)
            assertEquals(photos.map { it.id }, episode.photos.map { it.id })
            assertEquals("Rynok Square", vision.lastPlaceName)
        }

    @Test
    fun `passes the given language tag through to the vision client`() =
        runTest {
            val vision = FakeEpisodeVisionDescriptionClient()
            val pipeline =
                EpisodeProcessingPipeline(
                    geocodingClient = FakePlaceGeocodingClient(),
                    visionDescriptionClient = vision,
                    imageBytesLoader = { byteArrayOf(1) },
                    photoUploadClient = FakePhotoUploadClient(),
                    generateEpisodeId = { "episode-1" },
                )

            pipeline.process("entry-1", listOf(photo("p1", 0)), languageTag = "uk")

            assertEquals("uk", vision.lastLanguageTag)
        }

    @Test
    fun `every photo in the cluster gets an upload attempt not just the representative subset`() =
        runTest {
            val uploadClient = FakePhotoUploadClient()
            val pipeline =
                EpisodeProcessingPipeline(
                    geocodingClient = FakePlaceGeocodingClient(),
                    visionDescriptionClient = FakeEpisodeVisionDescriptionClient(),
                    imageBytesLoader = { byteArrayOf(1) },
                    photoUploadClient = uploadClient,
                    generateEpisodeId = { "episode-1" },
                )
            // 6 photos in one cluster - representative selection for vision caps at
            // MAX_REPRESENTATIVE_PHOTOS (4), but every photo must still get an upload attempt.
            val photos = (0..5).map { photo("p$it", it) }

            pipeline.process("entry-1", photos, "en")

            assertEquals(photos.map { it.id }.toSet(), uploadClient.uploaded.map { it.id }.toSet())
        }

    @Test
    fun `each photo's remoteUrl matches what the upload client returned`() =
        runTest {
            val pipeline =
                EpisodeProcessingPipeline(
                    geocodingClient = FakePlaceGeocodingClient(),
                    visionDescriptionClient = FakeEpisodeVisionDescriptionClient(),
                    imageBytesLoader = { byteArrayOf(1) },
                    photoUploadClient = FakePhotoUploadClient(),
                    generateEpisodeId = { "episode-1" },
                )
            val photos = listOf(photo("p1", 0), photo("p2", 10))

            val episode = pipeline.process("entry-1", photos, "en").single()

            assertEquals("https://storage/p1", episode.photos.single { it.id == "p1" }.remoteUrl)
            assertEquals("https://storage/p2", episode.photos.single { it.id == "p2" }.remoteUrl)
        }

    @Test
    fun `one photo's upload failure does not fail the whole episode`() =
        runTest {
            val uploadClient =
                FakePhotoUploadClient { photo ->
                    if (photo.id == "p2") {
                        PhotoUploadResult.Failure(RuntimeException("boom"))
                    } else {
                        PhotoUploadResult.Uploaded("https://storage/${photo.id}")
                    }
                }
            val pipeline =
                EpisodeProcessingPipeline(
                    geocodingClient = FakePlaceGeocodingClient(),
                    visionDescriptionClient = FakeEpisodeVisionDescriptionClient(),
                    imageBytesLoader = { byteArrayOf(1) },
                    photoUploadClient = uploadClient,
                    generateEpisodeId = { "episode-1" },
                )
            val photos = listOf(photo("p1", 0), photo("p2", 10), photo("p3", 20))

            val episodes = pipeline.process("entry-1", photos, "en")

            assertEquals(1, episodes.size)
            val resultPhotos = episodes.single().photos
            assertEquals("https://storage/p1", resultPhotos.single { it.id == "p1" }.remoteUrl)
            assertNull(resultPhotos.single { it.id == "p2" }.remoteUrl)
            assertEquals("https://storage/p3", resultPhotos.single { it.id == "p3" }.remoteUrl)
        }

    @Test
    fun `empty photo list produces no episodes`() =
        runTest {
            val pipeline =
                EpisodeProcessingPipeline(
                    geocodingClient = FakePlaceGeocodingClient(),
                    visionDescriptionClient = FakeEpisodeVisionDescriptionClient(),
                    imageBytesLoader = { byteArrayOf(1) },
                    photoUploadClient = FakePhotoUploadClient(),
                    generateEpisodeId = { "episode-1" },
                )

            assertEquals(emptyList(), pipeline.process("entry-1", emptyList(), "en"))
        }

    private fun incompleteEpisode(
        photos: List<Photo>,
        description: String? = null,
        descriptionAttempts: Int = 1,
    ) = Episode(
        id = "episode-1",
        diaryEntryId = "entry-1",
        startTime = baseTime,
        endTime = baseTime,
        latitude = 49.8397,
        longitude = 24.0297,
        placeName = "Rynok Square",
        description = description,
        descriptionAttempts = descriptionAttempts,
        photos = photos,
        syncStatus = SyncStatus.PENDING,
        updatedAt = FIXED_NOW,
    )

    @Test
    fun `retryIncomplete uploads a photo still missing its remoteUrl`() =
        runTest {
            val uploadClient = FakePhotoUploadClient()
            val pipeline =
                EpisodeProcessingPipeline(
                    geocodingClient = FakePlaceGeocodingClient(),
                    visionDescriptionClient = FakeEpisodeVisionDescriptionClient(),
                    imageBytesLoader = { byteArrayOf(1) },
                    photoUploadClient = uploadClient,
                    clock = FixedClock,
                )
            val episode =
                incompleteEpisode(
                    photos = listOf(photo("p1", 0)),
                    description = "already generated",
                )

            val retried = pipeline.retryIncomplete(episode, "en")

            assertEquals("https://storage/p1", retried.photos.single().remoteUrl)
            assertEquals("already generated", retried.description)
            // Only the missing photo upload was retried - no description attempt was made, so
            // the attempts counter (a description-only concern) is untouched.
            assertEquals(1, retried.descriptionAttempts)
        }

    @Test
    fun `retryIncomplete regenerates a missing description and counts the attempt`() =
        runTest {
            val vision = FakeEpisodeVisionDescriptionClient()
            val pipeline =
                EpisodeProcessingPipeline(
                    geocodingClient = FakePlaceGeocodingClient(),
                    visionDescriptionClient = vision,
                    imageBytesLoader = { byteArrayOf(1) },
                    photoUploadClient = FakePhotoUploadClient(),
                    clock = FixedClock,
                )
            val episode =
                incompleteEpisode(
                    photos = listOf(photo("p1", 0).copy(remoteUrl = "https://storage/p1")),
                    description = null,
                    descriptionAttempts = 1,
                )

            val retried = pipeline.retryIncomplete(episode, "en")

            assertEquals("A wander through the old town.", retried.description)
            assertEquals(2, retried.descriptionAttempts)
            assertEquals("Rynok Square", vision.lastPlaceName)
        }

    @Test
    fun `retryIncomplete leaves an already-complete episode untouched`() =
        runTest {
            val pipeline =
                EpisodeProcessingPipeline(
                    geocodingClient = FakePlaceGeocodingClient(),
                    visionDescriptionClient = FakeEpisodeVisionDescriptionClient(),
                    imageBytesLoader = { byteArrayOf(1) },
                    photoUploadClient = FakePhotoUploadClient(),
                    clock = FixedClock,
                )
            val episode =
                incompleteEpisode(
                    photos = listOf(photo("p1", 0).copy(remoteUrl = "https://storage/p1")),
                    description = "already generated",
                )

            assertEquals(episode, pipeline.retryIncomplete(episode, "en"))
        }

    @Test
    fun `retryIncomplete still failing keeps the photo unuploaded without throwing`() =
        runTest {
            val pipeline =
                EpisodeProcessingPipeline(
                    geocodingClient = FakePlaceGeocodingClient(),
                    visionDescriptionClient = FakeEpisodeVisionDescriptionClient(),
                    imageBytesLoader = { byteArrayOf(1) },
                    photoUploadClient = FakePhotoUploadClient { PhotoUploadResult.Failure(RuntimeException("still offline")) },
                    clock = FixedClock,
                )
            val episode = incompleteEpisode(photos = listOf(photo("p1", 0)), description = "already generated")

            val retried = pipeline.retryIncomplete(episode, "en")

            assertNull(retried.photos.single().remoteUrl)
        }
}
