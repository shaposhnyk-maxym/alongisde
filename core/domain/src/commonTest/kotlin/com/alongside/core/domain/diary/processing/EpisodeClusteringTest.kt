package com.alongside.core.domain.diary.processing

import com.alongside.core.model.diary.Photo
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class EpisodeClusteringTest {
    private val baseTime = Instant.fromEpochMilliseconds(1_752_600_000_000)

    // Two points on the equator differ only in longitude, so the great-circle distance between
    // them equals the longitude delta (in radians) times Earth's radius exactly - lets boundary
    // tests hit precise meter distances without relying on haversineDistanceMeters itself.
    private fun lonOffsetForMeters(meters: Double): Double = (meters / EARTH_RADIUS_METERS) * (180.0 / PI)

    private fun photo(
        id: String,
        takenAt: Instant,
        longitude: Double = 0.0,
    ) = Photo(id = id, uri = "content://photos/$id", takenAt = takenAt, latitude = 0.0, longitude = longitude)

    @Test
    fun `empty photo list produces no episodes`() {
        assertEquals(emptyList(), clusterPhotosIntoEpisodes(emptyList()))
    }

    @Test
    fun `single photo forms a single episode`() {
        val photo = photo("p1", baseTime)

        assertEquals(listOf(listOf(photo)), clusterPhotosIntoEpisodes(listOf(photo)))
    }

    @Test
    fun `photos close in time and place stay in one episode`() {
        val p1 = photo("p1", baseTime)
        val p2 = photo("p2", baseTime + 600.seconds)

        assertEquals(listOf(listOf(p1, p2)), clusterPhotosIntoEpisodes(listOf(p1, p2)))
    }

    @Test
    fun `input order does not matter - photos are sorted chronologically first`() {
        val p1 = photo("p1", baseTime)
        val p2 = photo("p2", baseTime + 600.seconds)

        assertEquals(listOf(listOf(p1, p2)), clusterPhotosIntoEpisodes(listOf(p2, p1)))
    }

    @Test
    fun `distance exactly at the 500m threshold stays in the same episode`() {
        val p1 = photo("p1", baseTime)
        val p2 = photo("p2", baseTime + 60.seconds, longitude = lonOffsetForMeters(EPISODE_DISTANCE_THRESHOLD_METERS))

        assertEquals(listOf(listOf(p1, p2)), clusterPhotosIntoEpisodes(listOf(p1, p2)))
    }

    @Test
    fun `distance just over the 500m threshold starts a new episode`() {
        val p1 = photo("p1", baseTime)
        val p2 =
            photo(
                "p2",
                baseTime + 60.seconds,
                longitude = lonOffsetForMeters(EPISODE_DISTANCE_THRESHOLD_METERS + 1.0),
            )

        assertEquals(listOf(listOf(p1), listOf(p2)), clusterPhotosIntoEpisodes(listOf(p1, p2)))
    }

    @Test
    fun `time gap exactly at the 2h threshold stays in the same episode`() {
        val p1 = photo("p1", baseTime)
        val p2 = photo("p2", baseTime + EPISODE_TIME_THRESHOLD)

        assertEquals(listOf(listOf(p1, p2)), clusterPhotosIntoEpisodes(listOf(p1, p2)))
    }

    @Test
    fun `time gap just over the 2h threshold starts a new episode`() {
        val p1 = photo("p1", baseTime)
        val p2 = photo("p2", baseTime + EPISODE_TIME_THRESHOLD + 1.seconds)

        assertEquals(listOf(listOf(p1), listOf(p2)), clusterPhotosIntoEpisodes(listOf(p1, p2)))
    }

    @Test
    fun `exceeding either threshold alone is enough to start a new episode`() {
        // Within distance, but time exceeded.
        val p1 = photo("p1", baseTime)
        val p2 = photo("p2", baseTime + EPISODE_TIME_THRESHOLD + 1.seconds)
        assertEquals(listOf(listOf(p1), listOf(p2)), clusterPhotosIntoEpisodes(listOf(p1, p2)))

        // Within time, but distance exceeded.
        val p3 = photo("p3", baseTime)
        val p4 =
            photo("p4", baseTime + 60.seconds, longitude = lonOffsetForMeters(EPISODE_DISTANCE_THRESHOLD_METERS + 50.0))
        assertEquals(listOf(listOf(p3), listOf(p4)), clusterPhotosIntoEpisodes(listOf(p3, p4)))
    }

    @Test
    fun `clustering is a sliding chain against the previous photo rather than a fixed anchor`() {
        // p1 -> p2 -> p3 each 400m apart (within threshold pairwise) but p1 -> p3 is 800m apart
        // (over threshold). Anchor-based clustering would split at p3; chain-based keeps it together.
        val p1 = photo("p1", baseTime, longitude = 0.0)
        val p2 = photo("p2", baseTime + 60.seconds, longitude = lonOffsetForMeters(400.0))
        val p3 = photo("p3", baseTime + 120.seconds, longitude = lonOffsetForMeters(800.0))

        assertEquals(listOf(listOf(p1, p2, p3)), clusterPhotosIntoEpisodes(listOf(p1, p2, p3)))
    }

    @Test
    fun `multiple synthetic photo sets group into the expected episodes`() {
        val p1 = photo("p1", baseTime)
        val p2 = photo("p2", baseTime + 600.seconds)
        val p3 = photo("p3", baseTime + 5.hours)
        val p4 = photo("p4", baseTime + 5.hours + 600.seconds)
        val p5 = photo("p5", baseTime + 5.hours + 1200.seconds)

        val episodes = clusterPhotosIntoEpisodes(listOf(p5, p3, p1, p4, p2))

        assertEquals(listOf(listOf(p1, p2), listOf(p3, p4, p5)), episodes)
    }
}
