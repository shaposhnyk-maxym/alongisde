package com.alongside.core.domain.recap

import com.alongside.core.model.diary.DiaryEntry
import com.alongside.core.model.diary.Episode
import com.alongside.core.model.place.PlaceCandidate
import com.alongside.core.model.place.PlaceSwipe
import com.alongside.core.model.place.SwipeDirection
import com.alongside.core.model.pretrip.PreTripPhoto
import com.alongside.core.model.recap.RecapSlide
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

private val TRIP_START = LocalDate(2026, 7, 18)
private val TRIP_END = LocalDate(2026, 7, 21)
private val DAY_1 = LocalDate(2026, 7, 18)
private val DAY_2 = LocalDate(2026, 7, 19)
private val DAY_4 = LocalDate(2026, 7, 21)

/** Everything a "fully populated" trip needs to produce every slide type at least once. */
private class PopulatedRecapData {
    val ownEntryDay1 = recapDiaryEntry("own-1", "own", DAY_1)
    val ownEntryDay2 = recapDiaryEntry("own-2", "own", DAY_2)
    val partnerEntryDay2 = recapDiaryEntry("partner-2", "partner", DAY_2)
    val partnerEntryDay4 = recapDiaryEntry("partner-4", "partner", DAY_4)

    val ownDiaryEntries = listOf(ownEntryDay1, ownEntryDay2)
    val partnerDiaryEntries = listOf(partnerEntryDay2, partnerEntryDay4)

    val episodesByDiaryEntryId =
        mapOf(
            ownEntryDay1.id to listOf(recapEpisode("ep-own-1", ownEntryDay1.id, city = "Lviv")),
            ownEntryDay2.id to
                listOf(
                    recapEpisode(
                        "ep-own-2",
                        ownEntryDay2.id,
                        startTime = recapInstant(1_000),
                        endTime = recapInstant(1_600),
                    ),
                ),
            partnerEntryDay2.id to
                listOf(
                    recapEpisode(
                        "ep-partner-2",
                        partnerEntryDay2.id,
                        startTime = recapInstant(1_300),
                        endTime = recapInstant(1_900),
                    ),
                ),
            partnerEntryDay4.id to
                listOf(
                    recapEpisode(
                        "ep-partner-4",
                        partnerEntryDay4.id,
                        startTime = recapInstant(2_000),
                        endTime = recapInstant(2_000),
                        city = "Kyiv",
                    ),
                ),
        )

    val ownPreTripPhotos = listOf(recapPreTripPhoto("own-photo-1", "own", recapInstant(0)))
    val partnerPreTripPhotos =
        listOf(recapPreTripPhoto("partner-photo-1", "partner", recapInstant(1), latitude = 10.0, longitude = 10.0))

    val matchedCandidate = recapPlaceCandidate("candidate-cafe", category = "cafe")
    val pendingCandidate = recapPlaceCandidate("candidate-museum", category = "museum")
    val placeCandidates = listOf(matchedCandidate, pendingCandidate)

    val ownPlaceSwipes =
        listOf(
            recapSwipe("s-own-cafe", matchedCandidate.id, "own", SwipeDirection.LIKE),
            recapSwipe("s-own-museum", pendingCandidate.id, "own", SwipeDirection.LIKE),
        )
    val partnerPlaceSwipes = listOf(recapSwipe("s-partner-cafe", matchedCandidate.id, "partner", SwipeDirection.LIKE))

    fun build(
        ownDiaryEntries: List<DiaryEntry> = this.ownDiaryEntries,
        partnerDiaryEntries: List<DiaryEntry> = this.partnerDiaryEntries,
        episodesByDiaryEntryId: Map<String, List<Episode>> = this.episodesByDiaryEntryId,
        ownPreTripPhotos: List<PreTripPhoto> = this.ownPreTripPhotos,
        partnerPreTripPhotos: List<PreTripPhoto> = this.partnerPreTripPhotos,
        placeCandidates: List<PlaceCandidate> = this.placeCandidates,
        ownPlaceSwipes: List<PlaceSwipe> = this.ownPlaceSwipes,
        partnerPlaceSwipes: List<PlaceSwipe> = this.partnerPlaceSwipes,
    ): List<RecapSlide> =
        buildRecapSlides(
            tripStartDate = TRIP_START,
            tripEndDate = TRIP_END,
            ownDiaryEntries = ownDiaryEntries,
            partnerDiaryEntries = partnerDiaryEntries,
            episodesByDiaryEntryId = episodesByDiaryEntryId,
            ownPreTripPhotos = ownPreTripPhotos,
            partnerPreTripPhotos = partnerPreTripPhotos,
            placeCandidates = placeCandidates,
            ownPlaceSwipes = ownPlaceSwipes,
            partnerPlaceSwipes = partnerPlaceSwipes,
        )
}

class RecapSlideBuilderTest {
    @Test
    fun `a full trip produces the correct day-slide count a matched place in the match list and Final last`() {
        val data = PopulatedRecapData()

        val slides = data.build()

        val dayHighlights = slides.filterIsInstance<RecapSlide.DayHighlight>()
        assertEquals(3, dayHighlights.size)
        assertEquals(listOf(1, 2, 4), dayHighlights.map { it.dayIndex })

        val matchList = slides.filterIsInstance<RecapSlide.MatchList>().single()
        assertEquals(listOf(data.matchedCandidate), matchList.candidates)

        assertIs<RecapSlide.Final>(slides.last())
        assertEquals(4, (slides.last() as RecapSlide.Final).daysTogether)
    }

    @Test
    fun `a trip with zero episodes zero pre-trip photos and zero swipes on both sides is exactly Intro then Final`() {
        val slides =
            buildRecapSlides(
                tripStartDate = TRIP_START,
                tripEndDate = TRIP_END,
                ownDiaryEntries = emptyList(),
                partnerDiaryEntries = emptyList(),
                episodesByDiaryEntryId = emptyMap(),
                ownPreTripPhotos = emptyList(),
                partnerPreTripPhotos = emptyList(),
                placeCandidates = emptyList(),
                ownPlaceSwipes = emptyList(),
                partnerPlaceSwipes = emptyList(),
            )

        assertEquals(2, slides.size)
        assertIs<RecapSlide.Intro>(slides[0])
        assertIs<RecapSlide.Final>(slides[1])
    }

    @Test
    fun `Intro lists cities in chronological order of first appearance deduplicated`() {
        val data = PopulatedRecapData()

        val intro = data.build().filterIsInstance<RecapSlide.Intro>().single()

        assertEquals(listOf("Lviv", "Kyiv"), intro.cities)
    }

    @Test
    fun `Final is trip end date minus start date plus one`() {
        val slides =
            buildRecapSlides(
                tripStartDate = LocalDate(2026, 7, 18),
                tripEndDate = LocalDate(2026, 7, 25),
                ownDiaryEntries = emptyList(),
                partnerDiaryEntries = emptyList(),
                episodesByDiaryEntryId = emptyMap(),
                ownPreTripPhotos = emptyList(),
                partnerPreTripPhotos = emptyList(),
                placeCandidates = emptyList(),
                ownPlaceSwipes = emptyList(),
                partnerPlaceSwipes = emptyList(),
            )

        assertEquals(8, (slides.last() as RecapSlide.Final).daysTogether)
    }

    @Test
    fun `a date with no episodes from either side produces no DayHighlight for that date others unaffected`() {
        val data = PopulatedRecapData()

        val dayHighlights = data.build().filterIsInstance<RecapSlide.DayHighlight>()

        assertTrue(dayHighlights.none { it.date == LocalDate(2026, 7, 20) })
        assertEquals(3, dayHighlights.size)
    }

    @Test
    fun `no pre-trip photos on either side removes only ParallelLives`() {
        val data = PopulatedRecapData()

        val slides = data.build(ownPreTripPhotos = emptyList(), partnerPreTripPhotos = emptyList())

        assertTrue(slides.none { it is RecapSlide.ParallelLives })
        assertTrue(slides.any { it is RecapSlide.DayHighlight })
        assertTrue(slides.any { it is RecapSlide.MatchList })
    }

    @Test
    fun `pre-trip photos on only one side removes ParallelLives`() {
        val data = PopulatedRecapData()

        val slides = data.build(partnerPreTripPhotos = emptyList())

        assertTrue(slides.none { it is RecapSlide.ParallelLives })
    }

    @Test
    fun `no overlapping episodes anywhere in the trip removes only ClosestMoment`() {
        val data = PopulatedRecapData()
        val nonOverlappingEpisodes =
            data.episodesByDiaryEntryId +
                mapOf(
                    data.ownEntryDay2.id to
                        listOf(
                            recapEpisode(
                                "ep-own-2",
                                data.ownEntryDay2.id,
                                startTime = recapInstant(0),
                                endTime = recapInstant(100),
                            ),
                        ),
                    data.partnerEntryDay2.id to
                        listOf(
                            recapEpisode(
                                "ep-partner-2",
                                data.partnerEntryDay2.id,
                                startTime = recapInstant(500),
                                endTime = recapInstant(600),
                            ),
                        ),
                )

        val slides = data.build(episodesByDiaryEntryId = nonOverlappingEpisodes)

        assertTrue(slides.none { it is RecapSlide.ClosestMoment })
        assertTrue(slides.any { it is RecapSlide.DayHighlight })
    }

    @Test
    fun `no categorized swipes on either side removes only SwipeArchetype`() {
        val data = PopulatedRecapData()

        val slides = data.build(ownPlaceSwipes = emptyList(), partnerPlaceSwipes = emptyList())

        assertTrue(slides.none { it is RecapSlide.SwipeArchetype })
        assertTrue(slides.any { it is RecapSlide.DayHighlight })
    }

    @Test
    fun `categorized swipes on only one side removes SwipeArchetype`() {
        val data = PopulatedRecapData()

        val slides = data.build(partnerPlaceSwipes = emptyList())

        assertTrue(slides.none { it is RecapSlide.SwipeArchetype })
        // The one-sided candidate now also has no match status contributor on the partner side,
        // so both candidates end up PENDING - MatchList naturally disappears alongside it.
        assertTrue(slides.none { it is RecapSlide.MatchList })
        assertTrue(slides.any { it is RecapSlide.UnresolvedQuestion })
    }

    @Test
    fun `no PENDING candidates removes only UnresolvedQuestion`() {
        val data = PopulatedRecapData()

        val slides = data.build(placeCandidates = listOf(data.matchedCandidate))

        assertTrue(slides.none { it is RecapSlide.UnresolvedQuestion })
        assertTrue(slides.any { it is RecapSlide.MatchList })
    }

    @Test
    fun `zero MATCHED candidates removes MatchList without crashing or rendering an empty grid`() {
        val data = PopulatedRecapData()

        val slides = data.build(partnerPlaceSwipes = emptyList())

        assertTrue(slides.none { it is RecapSlide.MatchList })
    }

    @Test
    fun `no diary entries or episodes on either side removes only DayHighlight and ClosestMoment`() {
        val data = PopulatedRecapData()

        val slides =
            data.build(
                ownDiaryEntries = emptyList(),
                partnerDiaryEntries = emptyList(),
                episodesByDiaryEntryId = emptyMap(),
            )

        assertTrue(slides.none { it is RecapSlide.DayHighlight })
        assertTrue(slides.none { it is RecapSlide.ClosestMoment })
        assertTrue(slides.any { it is RecapSlide.ParallelLives })
        assertTrue(slides.any { it is RecapSlide.MatchList })
    }
}
