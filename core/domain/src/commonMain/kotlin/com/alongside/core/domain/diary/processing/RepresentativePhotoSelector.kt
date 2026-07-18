package com.alongside.core.domain.diary.processing

import com.alongside.core.model.diary.Photo

public const val MIN_REPRESENTATIVE_PHOTOS: Int = 2
public const val MAX_REPRESENTATIVE_PHOTOS: Int = 4

/**
 * Picks a representative subset of [photos] for the Gemini vision call: always the earliest and
 * latest photo, then fills remaining slots (up to [maxCount]) greedily by greatest additional
 * time-spread from what's already selected. Returns all photos as-is when there are [minCount] or
 * fewer - nothing to trim down.
 */
public fun selectRepresentativePhotos(
    photos: List<Photo>,
    minCount: Int = MIN_REPRESENTATIVE_PHOTOS,
    maxCount: Int = MAX_REPRESENTATIVE_PHOTOS,
): List<Photo> {
    val sorted = photos.sortedBy { it.takenAt }
    if (sorted.size <= minCount) return sorted

    val targetCount = minOf(maxCount, sorted.size)
    val selected = mutableListOf(sorted.first(), sorted.last())
    val remaining = sorted.subList(1, sorted.size - 1).toMutableList()
    while (selected.size < targetCount && remaining.isNotEmpty()) {
        val next = remaining.maxBy { candidate -> selected.minOf { (candidate.takenAt - it.takenAt).absoluteValue } }
        selected += next
        remaining -= next
    }
    return selected.sortedBy { it.takenAt }
}
