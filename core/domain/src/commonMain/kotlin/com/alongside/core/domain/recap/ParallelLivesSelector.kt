package com.alongside.core.domain.recap

import com.alongside.core.domain.diary.processing.haversineDistanceMeters
import com.alongside.core.model.pretrip.PreTripPhoto
import com.alongside.core.model.recap.RecapSlide

/**
 * For each own photo, pairs it with the partner photo nearest in time, then picks the resulting
 * pair with the **maximum** haversine distance apart - the most dramatic "parallel lives" split.
 * Absent if either side has zero pre-trip photos.
 */
public fun selectParallelLivesPair(
    ownPhotos: List<PreTripPhoto>,
    partnerPhotos: List<PreTripPhoto>,
): RecapSlide.ParallelLives? {
    if (ownPhotos.isEmpty() || partnerPhotos.isEmpty()) return null
    return ownPhotos
        .map { own ->
            val partner = nearestInTime(own, partnerPhotos)
            val distance = haversineDistanceMeters(own.latitude, own.longitude, partner.latitude, partner.longitude)
            RecapSlide.ParallelLives(own, partner, distance)
        }.maxBy { it.distanceMeters }
}

private fun nearestInTime(
    own: PreTripPhoto,
    candidates: List<PreTripPhoto>,
): PreTripPhoto =
    candidates.reduce { closest, next ->
        val nextGap = (own.takenAt - next.takenAt).absoluteValue
        val closestGap = (own.takenAt - closest.takenAt).absoluteValue
        if (nextGap < closestGap) next else closest
    }
