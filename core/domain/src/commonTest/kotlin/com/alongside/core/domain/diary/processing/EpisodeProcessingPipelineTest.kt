package com.alongside.core.domain.diary.processing

import com.alongside.core.model.diary.Photo
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/** Fake [PlaceGeocodingClient] - scriptable per call, records the queried coordinates. */
private class FakePlaceGeocodingClient(
    private val result: GeocodingResult = GeocodingResult.Found("Rynok Square"),
) : PlaceGeocodingClient {
    val queries = mutableListOf<Pair<Double, Double>>()

    override suspend fun reverseGeocode(
        latitude: Double,
        longitude: Double,
    ): GeocodingResult {
        queries += latitude to longitude
        return result
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
                    generateEpisodeId = { "episode-1" },
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
            assertEquals(photos, episode.photos)
            assertEquals(1, geocoding.queries.size)
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
                    generateEpisodeId = { ids.next() },
                )
            val photos = listOf(photo("p1", 0), photo("p2", 300))

            val episodes = pipeline.process(diaryEntryId = "entry-1", photos = photos, languageTag = "en")

            assertEquals(2, episodes.size)
        }

    @Test
    fun `geocoding NotFound leaves placeName null but still generates a description`() =
        runTest {
            val pipeline =
                EpisodeProcessingPipeline(
                    geocodingClient = FakePlaceGeocodingClient(result = GeocodingResult.NotFound),
                    visionDescriptionClient = FakeEpisodeVisionDescriptionClient(),
                    imageBytesLoader = { byteArrayOf(1) },
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
                    generateEpisodeId = { "episode-1" },
                )
            // 6 photos in one cluster - representative selection caps at MAX_REPRESENTATIVE_PHOTOS (4).
            val photos = (0..5).map { photo("p$it", it) }

            val episode = pipeline.process("entry-1", photos, "uk").single()

            assertEquals(MAX_REPRESENTATIVE_PHOTOS, vision.lastImageCount)
            assertEquals(photos, episode.photos)
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
                    generateEpisodeId = { "episode-1" },
                )

            pipeline.process("entry-1", listOf(photo("p1", 0)), languageTag = "uk")

            assertEquals("uk", vision.lastLanguageTag)
        }

    @Test
    fun `empty photo list produces no episodes`() =
        runTest {
            val pipeline =
                EpisodeProcessingPipeline(
                    geocodingClient = FakePlaceGeocodingClient(),
                    visionDescriptionClient = FakeEpisodeVisionDescriptionClient(),
                    imageBytesLoader = { byteArrayOf(1) },
                    generateEpisodeId = { "episode-1" },
                )

            assertEquals(emptyList(), pipeline.process("entry-1", emptyList(), "en"))
        }
}
