package com.alongside.core.domain.recap

import com.alongside.core.domain.place.resolveMatchStatus
import com.alongside.core.model.diary.DiaryEntry
import com.alongside.core.model.diary.Episode
import com.alongside.core.model.place.MatchStatus
import com.alongside.core.model.place.PlaceCandidate
import com.alongside.core.model.place.PlaceSwipe
import com.alongside.core.model.pretrip.PreTripPhoto
import com.alongside.core.model.recap.RecapSlide
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil

/**
 * Assembles the full end-of-trip recap deck (docs/roadmap.md M20) from already-synced data - same
 * "caller pre-splits own/partner lists" shape as
 * [com.alongside.core.domain.diary.buildDiaryTimelineDays]. Each slide type is its own pure
 * function of the available data: missing its minimum simply omits that slide, nothing here ever
 * throws or renders a placeholder for "no data". `Intro` and `Final` are the only two always
 * present - a trip nobody used still produces `[Intro, Final]`.
 */
public fun buildRecapSlides(
    tripStartDate: LocalDate,
    tripEndDate: LocalDate,
    ownDiaryEntries: List<DiaryEntry>,
    partnerDiaryEntries: List<DiaryEntry>,
    episodesByDiaryEntryId: Map<String, List<Episode>>,
    ownPreTripPhotos: List<PreTripPhoto>,
    partnerPreTripPhotos: List<PreTripPhoto>,
    placeCandidates: List<PlaceCandidate>,
    ownPlaceSwipes: List<PlaceSwipe>,
    partnerPlaceSwipes: List<PlaceSwipe>,
): List<RecapSlide> {
    val dates = tripDates(tripStartDate, tripEndDate)
    val ownByDate = ownDiaryEntries.associateBy { it.date }
    val partnerByDate = partnerDiaryEntries.associateBy { it.date }

    val slides = mutableListOf<RecapSlide>()

    slides += RecapSlide.Intro(buildIntroCities(ownDiaryEntries, partnerDiaryEntries, episodesByDiaryEntryId))

    selectParallelLivesPair(ownPreTripPhotos, partnerPreTripPhotos)?.let { slides += it }

    slides += buildDayHighlights(dates, ownByDate, partnerByDate, episodesByDiaryEntryId)

    findClosestMoment(dates, ownByDate, partnerByDate, episodesByDiaryEntryId)?.let { slides += it }

    buildSwipeArchetype(placeCandidates, ownPlaceSwipes, partnerPlaceSwipes)?.let { slides += it }

    val matchStatusByCandidate = resolveMatchStatuses(placeCandidates, ownPlaceSwipes, partnerPlaceSwipes)
    val unresolvedCandidates = matchStatusByCandidate.filterValues { it == MatchStatus.PENDING }.keys.toList()
    if (unresolvedCandidates.isNotEmpty()) slides += RecapSlide.UnresolvedQuestion(unresolvedCandidates)
    val matchedCandidates = matchStatusByCandidate.filterValues { it == MatchStatus.MATCHED }.keys.toList()
    if (matchedCandidates.isNotEmpty()) slides += RecapSlide.MatchList(matchedCandidates)

    slides += RecapSlide.Final(daysTogether(tripStartDate, tripEndDate))

    return slides
}

/** "X days together" - always present, always last. */
public fun daysTogether(
    tripStartDate: LocalDate,
    tripEndDate: LocalDate,
): Int = tripStartDate.daysUntil(tripEndDate) + 1

private fun buildIntroCities(
    ownDiaryEntries: List<DiaryEntry>,
    partnerDiaryEntries: List<DiaryEntry>,
    episodesByDiaryEntryId: Map<String, List<Episode>>,
): List<String> =
    (ownDiaryEntries + partnerDiaryEntries)
        .flatMap { entry -> episodesByDiaryEntryId[entry.id].orEmpty() }
        .sortedBy { it.startTime }
        .mapNotNull { episode -> episode.city?.let { it to episode.countryCode } }
        .distinct()
        .map { (city, _) -> city }

private fun buildDayHighlights(
    dates: List<LocalDate>,
    ownByDate: Map<LocalDate, DiaryEntry>,
    partnerByDate: Map<LocalDate, DiaryEntry>,
    episodesByDiaryEntryId: Map<String, List<Episode>>,
): List<RecapSlide.DayHighlight> =
    dates.mapIndexedNotNull { index, date ->
        val ownEpisodes = episodesOnDate(ownByDate, episodesByDiaryEntryId, date)
        val partnerEpisodes = episodesOnDate(partnerByDate, episodesByDiaryEntryId, date)
        if (ownEpisodes.isEmpty() && partnerEpisodes.isEmpty()) {
            null
        } else {
            RecapSlide.DayHighlight(date, index + 1, ownEpisodes, partnerEpisodes)
        }
    }

private fun resolveMatchStatuses(
    placeCandidates: List<PlaceCandidate>,
    ownPlaceSwipes: List<PlaceSwipe>,
    partnerPlaceSwipes: List<PlaceSwipe>,
): Map<PlaceCandidate, MatchStatus> {
    val ownSwipesByCandidateId = ownPlaceSwipes.associateBy { it.candidateId }
    val partnerSwipesByCandidateId = partnerPlaceSwipes.associateBy { it.candidateId }
    return placeCandidates.associateWith { candidate ->
        resolveMatchStatus(
            ownerSwipe = ownSwipesByCandidateId[candidate.id]?.direction,
            memberSwipe = partnerSwipesByCandidateId[candidate.id]?.direction,
        )
    }
}
