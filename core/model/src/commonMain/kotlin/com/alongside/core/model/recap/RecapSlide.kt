package com.alongside.core.model.recap

import com.alongside.core.model.diary.Episode
import com.alongside.core.model.place.PlaceCandidate
import com.alongside.core.model.pretrip.PreTripPhoto
import kotlinx.datetime.LocalDate

/**
 * One slide of the end-of-trip Stories-format recap (docs/roadmap.md M20). Each variant is a pure
 * data holder for whatever its rendering needs - no UI types, no knowledge of how it gets drawn.
 * `core:domain`'s `buildRecapSlides` is the only place that decides whether a given variant
 * appears at all for a trip.
 */
public sealed interface RecapSlide {
    /** Always present, even when [cities] is empty - the deck's opening slide with the route. */
    public data class Intro(
        val cities: List<String>,
    ) : RecapSlide

    /**
     * "Parallel lives" comparison: the pre-trip photo pair (one per side) with the greatest
     * haversine distance apart, among each own photo's nearest-in-time partner photo.
     */
    public data class ParallelLives(
        val ownPhoto: PreTripPhoto,
        val partnerPhoto: PreTripPhoto,
        val distanceMeters: Double,
    ) : RecapSlide

    /** One per trip date with at least one [Episode] from either side. [dayIndex] is calendar-based. */
    public data class DayHighlight(
        val date: LocalDate,
        val dayIndex: Int,
        val ownEpisodes: List<Episode>,
        val partnerEpisodes: List<Episode>,
    ) : RecapSlide

    /**
     * The single closest-together moment across the whole trip: the overlapping-time episode pair
     * (one per side) with the smallest haversine distance between them.
     */
    public data class ClosestMoment(
        val date: LocalDate,
        val ownEpisode: Episode,
        val partnerEpisode: Episode,
        val distanceMeters: Double,
    ) : RecapSlide

    /** Like/dislike distribution by [PlaceCandidate.category], separately per side. */
    public data class SwipeArchetype(
        val ownTally: List<CategorySwipeTally>,
        val partnerTally: List<CategorySwipeTally>,
    ) : RecapSlide

    /** Candidates whose match status is still PENDING (including one-sided) at generation time. */
    public data class UnresolvedQuestion(
        val candidates: List<PlaceCandidate>,
    ) : RecapSlide

    /** Candidates both sides liked. */
    public data class MatchList(
        val candidates: List<PlaceCandidate>,
    ) : RecapSlide

    /** Always present, always last - "X days together". */
    public data class Final(
        val daysTogether: Int,
    ) : RecapSlide
}

/** One [PlaceCandidate.category]'s swipe counts for one side, part of [RecapSlide.SwipeArchetype]. */
public data class CategorySwipeTally(
    val category: String,
    val likeCount: Int,
    val dislikeCount: Int,
)
