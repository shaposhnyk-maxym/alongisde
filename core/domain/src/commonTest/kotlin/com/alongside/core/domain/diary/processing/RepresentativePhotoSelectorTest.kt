package com.alongside.core.domain.diary.processing

import com.alongside.core.model.diary.Photo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class RepresentativePhotoSelectorTest {
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
    fun `zero photos returns an empty list`() {
        assertEquals(emptyList(), selectRepresentativePhotos(emptyList()))
    }

    @Test
    fun `fewer photos than the minimum returns all of them unmodified`() {
        val single = photo("p1", 0)

        assertEquals(listOf(single), selectRepresentativePhotos(listOf(single)))
    }

    @Test
    fun `exactly the minimum count returns all of them`() {
        val p1 = photo("p1", 0)
        val p2 = photo("p2", 10)

        assertEquals(listOf(p1, p2), selectRepresentativePhotos(listOf(p1, p2)))
    }

    @Test
    fun `always includes the earliest and latest photo`() {
        val p1 = photo("p1", 0)
        val p2 = photo("p2", 10)
        val p3 = photo("p3", 20)
        val p4 = photo("p4", 30)
        val p5 = photo("p5", 40)

        val selected = selectRepresentativePhotos(listOf(p3, p1, p5, p2, p4))

        assertEquals(p1, selected.first())
        assertEquals(p5, selected.last())
    }

    @Test
    fun `never selects more than the max count even with many photos`() {
        val photos = (0..20).map { photo("p$it", it * 5) }

        val selected = selectRepresentativePhotos(photos)

        assertEquals(4, selected.size)
    }

    @Test
    fun `selection is returned in chronological order`() {
        val photos = (0..10).map { photo("p$it", it * 5) }

        val selected = selectRepresentativePhotos(photos)

        assertEquals(selected.sortedBy { it.takenAt }, selected)
    }

    @Test
    fun `picks photos maximizing spread of capture times rather than the first few`() {
        // Photos cluster at the start (0,1,2 min apart) then one far outlier at 100min.
        // Greatest-time-difference selection should prefer the outlier over a third near-start photo.
        val p1 = photo("p1", 0)
        val p2 = photo("p2", 1)
        val p3 = photo("p3", 2)
        val p4 = photo("p4", 100)

        val selected = selectRepresentativePhotos(listOf(p1, p2, p3, p4), maxCount = 3)

        assertEquals(setOf(p1, p4), setOf(selected.first(), selected.last()))
        assertEquals(3, selected.size)
    }
}
