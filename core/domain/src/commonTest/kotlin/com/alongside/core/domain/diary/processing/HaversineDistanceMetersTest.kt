package com.alongside.core.domain.diary.processing

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val DISTANCE_TOLERANCE_METERS = 50.0

class HaversineDistanceMetersTest {
    @Test
    fun `the same point is zero meters away`() {
        assertEquals(0.0, haversineDistanceMeters(49.8397, 24.0297, 49.8397, 24.0297))
    }

    @Test
    fun `Lviv to Kyiv is roughly 470km`() {
        val distance = haversineDistanceMeters(49.8397, 24.0297, 50.4501, 30.5234)

        assertTrue(abs(distance - 470_000.0) < 10_000.0)
    }

    @Test
    fun `one degree of longitude at the equator is roughly 111point2km on this function's spherical Earth model`() {
        // 2*pi*EARTH_RADIUS_METERS / 360 - the exact value for this function's assumed sphere
        // (mean radius 6_371_000m), not the WGS84 ellipsoid's ~111.32km equatorial figure.
        val distance = haversineDistanceMeters(0.0, 0.0, 0.0, 1.0)

        assertTrue(abs(distance - 111_194.9) < DISTANCE_TOLERANCE_METERS)
    }

    @Test
    fun `distance is symmetric regardless of argument order`() {
        val forward = haversineDistanceMeters(49.8397, 24.0297, 50.4501, 30.5234)
        val backward = haversineDistanceMeters(50.4501, 30.5234, 49.8397, 24.0297)

        assertEquals(forward, backward)
    }
}
