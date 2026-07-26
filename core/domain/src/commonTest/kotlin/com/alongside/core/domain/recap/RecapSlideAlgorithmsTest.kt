package com.alongside.core.domain.recap

import com.alongside.core.model.place.SwipeDirection
import com.alongside.core.model.recap.CategorySwipeTally
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SelectParallelLivesPairTest {
    @Test
    fun `no own photos yields no pair`() {
        val result =
            selectParallelLivesPair(
                ownPhotos = emptyList(),
                partnerPhotos = listOf(recapPreTripPhoto("partner-1", "partner", recapInstant(0))),
            )

        assertNull(result)
    }

    @Test
    fun `no partner photos yields no pair`() {
        val result =
            selectParallelLivesPair(
                ownPhotos = listOf(recapPreTripPhoto("own-1", "own", recapInstant(0))),
                partnerPhotos = emptyList(),
            )

        assertNull(result)
    }

    @Test
    fun `picks the nearest-in-time partner photo per own photo then the pair with maximum distance`() {
        val ownNear = recapPreTripPhoto("own-near", "own", recapInstant(0), latitude = 0.0, longitude = 0.0)
        val ownFar = recapPreTripPhoto("own-far", "own", recapInstant(100), latitude = 0.0, longitude = 0.0)
        val partnerCloseInSpace = recapPreTripPhoto("partner-close", "partner", recapInstant(1), latitude = 0.0, longitude = 0.0)
        val partnerFarInSpace = recapPreTripPhoto("partner-far", "partner", recapInstant(101), latitude = 10.0, longitude = 10.0)

        val result =
            selectParallelLivesPair(
                ownPhotos = listOf(ownNear, ownFar),
                partnerPhotos = listOf(partnerCloseInSpace, partnerFarInSpace),
            )

        assertEquals(ownFar, result?.ownPhoto)
        assertEquals(partnerFarInSpace, result?.partnerPhoto)
    }
}

class FindClosestMomentTest {
    private val date = LocalDate(2026, 7, 18)

    @Test
    fun `non-overlapping episodes yield no closest moment`() {
        val ownEntry = recapDiaryEntry("own-1", "own", date)
        val partnerEntry = recapDiaryEntry("partner-1", "partner", date)
        val ownEpisode = recapEpisode("ep-own", ownEntry.id, startTime = recapInstant(0), endTime = recapInstant(100))
        val partnerEpisode = recapEpisode("ep-partner", partnerEntry.id, startTime = recapInstant(200), endTime = recapInstant(300))

        val result =
            findClosestMoment(
                tripStartDate = date,
                tripEndDate = date,
                ownDiaryEntries = listOf(ownEntry),
                partnerDiaryEntries = listOf(partnerEntry),
                episodesByDiaryEntryId =
                    mapOf(
                        ownEntry.id to listOf(ownEpisode),
                        partnerEntry.id to listOf(partnerEpisode),
                    ),
            )

        assertNull(result)
    }

    @Test
    fun `among overlapping pairs the minimum distance one wins`() {
        val ownEntry = recapDiaryEntry("own-1", "own", date)
        val partnerEntry = recapDiaryEntry("partner-1", "partner", date)
        val ownNear =
            recapEpisode(
                "own-near",
                ownEntry.id,
                startTime = recapInstant(0),
                endTime = recapInstant(100),
                latitude = 0.0,
                longitude = 0.0,
            )
        val ownFar =
            recapEpisode(
                "own-far",
                ownEntry.id,
                startTime = recapInstant(0),
                endTime = recapInstant(100),
                latitude = 10.0,
                longitude = 10.0,
            )
        val partnerNear =
            recapEpisode(
                "partner-near",
                partnerEntry.id,
                startTime = recapInstant(50),
                endTime = recapInstant(150),
                latitude = 0.0,
                longitude = 0.0,
            )

        val result =
            findClosestMoment(
                tripStartDate = date,
                tripEndDate = date,
                ownDiaryEntries = listOf(ownEntry),
                partnerDiaryEntries = listOf(partnerEntry),
                episodesByDiaryEntryId =
                    mapOf(
                        ownEntry.id to listOf(ownNear, ownFar),
                        partnerEntry.id to listOf(partnerNear),
                    ),
            )

        assertEquals(ownNear, result?.ownEpisode)
        assertEquals(partnerNear, result?.partnerEpisode)
        assertEquals(0.0, result?.distanceMeters)
    }
}

class BuildSwipeArchetypeTest {
    @Test
    fun `no categorized swipes on either side yields no archetype`() {
        val result =
            buildSwipeArchetype(
                placeCandidates = emptyList(),
                ownSwipes = emptyList(),
                partnerSwipes = emptyList(),
            )

        assertNull(result)
    }

    @Test
    fun `categorized swipes on only one side yields no archetype`() {
        val cafe = recapPlaceCandidate("cafe-1", category = "cafe")

        val result =
            buildSwipeArchetype(
                placeCandidates = listOf(cafe),
                ownSwipes = listOf(recapSwipe("s1", cafe.id, "own", SwipeDirection.LIKE)),
                partnerSwipes = emptyList(),
            )

        assertNull(result)
    }

    @Test
    fun `tallies likes and dislikes per category separately for each side ignoring uncategorized candidates`() {
        val cafe1 = recapPlaceCandidate("cafe-1", category = "cafe")
        val cafe2 = recapPlaceCandidate("cafe-2", category = "cafe")
        val museum = recapPlaceCandidate("museum-1", category = "museum")
        val uncategorized = recapPlaceCandidate("mystery-1", category = null)

        val result =
            buildSwipeArchetype(
                placeCandidates = listOf(cafe1, cafe2, museum, uncategorized),
                ownSwipes =
                    listOf(
                        recapSwipe("s1", cafe1.id, "own", SwipeDirection.LIKE),
                        recapSwipe("s2", cafe2.id, "own", SwipeDirection.DISLIKE),
                        recapSwipe("s3", museum.id, "own", SwipeDirection.LIKE),
                        recapSwipe("s4", uncategorized.id, "own", SwipeDirection.LIKE),
                    ),
                partnerSwipes = listOf(recapSwipe("s5", cafe1.id, "partner", SwipeDirection.LIKE)),
            )

        assertEquals(
            setOf(
                CategorySwipeTally("cafe", likeCount = 1, dislikeCount = 1),
                CategorySwipeTally("museum", likeCount = 1, dislikeCount = 0),
            ),
            result?.ownTally?.toSet(),
        )
        assertEquals(
            setOf(CategorySwipeTally("cafe", likeCount = 1, dislikeCount = 0)),
            result?.partnerTally?.toSet(),
        )
    }
}
