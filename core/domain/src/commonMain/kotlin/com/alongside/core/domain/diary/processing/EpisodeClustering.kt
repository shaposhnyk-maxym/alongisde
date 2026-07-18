package com.alongside.core.domain.diary.processing

import com.alongside.core.model.diary.Photo
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

public val EPISODE_TIME_THRESHOLD: Duration = 2.hours
public const val EPISODE_DISTANCE_THRESHOLD_METERS: Double = 500.0

internal const val EARTH_RADIUS_METERS: Double = 6_371_000.0

// Floating-point trig round-trips (degrees -> radians -> haversine) land a few ULPs off an exact
// meter value depending on platform math-library rounding - without this, a photo placed exactly
// at the threshold via lon/lat arithmetic can compute as e.g. 500.00000000000006 and flip sides.
private const val DISTANCE_EPSILON_METERS: Double = 1e-6

private fun degreesToRadians(degrees: Double): Double = degrees * PI / 180.0

/** Great-circle distance between two lat/lon points, in meters. */
public fun haversineDistanceMeters(
    lat1: Double,
    lon1: Double,
    lat2: Double,
    lon2: Double,
): Double {
    val dLat = degreesToRadians(lat2 - lat1)
    val dLon = degreesToRadians(lon2 - lon1)
    val a =
        sin(dLat / 2) * sin(dLat / 2) +
            cos(degreesToRadians(lat1)) * cos(degreesToRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return EARTH_RADIUS_METERS * c
}

/**
 * Groups photos into episodes by chronological sliding-chain proximity: a photo continues the
 * current episode only while it is within [EPISODE_TIME_THRESHOLD] AND [EPISODE_DISTANCE_THRESHOLD_METERS]
 * of the *previous* photo (not a fixed anchor) - exceeding either threshold starts a new episode.
 */
public fun clusterPhotosIntoEpisodes(photos: List<Photo>): List<List<Photo>> {
    if (photos.isEmpty()) return emptyList()
    val sorted = photos.sortedBy { it.takenAt }
    val episodes = mutableListOf(mutableListOf(sorted.first()))
    for (photo in sorted.drop(1)) {
        val previous = episodes.last().last()
        val timeGap = photo.takenAt - previous.takenAt
        val distance = haversineDistanceMeters(previous.latitude, previous.longitude, photo.latitude, photo.longitude)
        val withinDistance = distance <= EPISODE_DISTANCE_THRESHOLD_METERS + DISTANCE_EPSILON_METERS
        if (timeGap <= EPISODE_TIME_THRESHOLD && withinDistance) {
            episodes.last().add(photo)
        } else {
            episodes.add(mutableListOf(photo))
        }
    }
    return episodes
}
