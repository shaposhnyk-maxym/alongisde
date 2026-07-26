package com.alongside.core.domain.recap

import com.alongside.core.domain.diary.processing.haversineDistanceMeters
import com.alongside.core.model.diary.DiaryEntry
import com.alongside.core.model.diary.Episode
import com.alongside.core.model.recap.RecapSlide
import kotlinx.datetime.LocalDate

/**
 * The single closest-together moment across the whole trip: among every (own, partner) episode
 * pair whose `startTime..endTime` ranges overlap, the one with the **minimum** haversine distance.
 * Absent if no such overlapping pair exists anywhere in the trip.
 */
public fun findClosestMoment(
    tripStartDate: LocalDate,
    tripEndDate: LocalDate,
    ownDiaryEntries: List<DiaryEntry>,
    partnerDiaryEntries: List<DiaryEntry>,
    episodesByDiaryEntryId: Map<String, List<Episode>>,
): RecapSlide.ClosestMoment? =
    findClosestMoment(
        tripDates(tripStartDate, tripEndDate),
        ownDiaryEntries.associateBy { it.date },
        partnerDiaryEntries.associateBy { it.date },
        episodesByDiaryEntryId,
    )

internal fun findClosestMoment(
    dates: List<LocalDate>,
    ownByDate: Map<LocalDate, DiaryEntry>,
    partnerByDate: Map<LocalDate, DiaryEntry>,
    episodesByDiaryEntryId: Map<String, List<Episode>>,
): RecapSlide.ClosestMoment? =
    dates
        .flatMap { date -> overlappingMomentsOnDate(date, ownByDate, partnerByDate, episodesByDiaryEntryId) }
        .minByOrNull { it.distanceMeters }

private fun overlappingMomentsOnDate(
    date: LocalDate,
    ownByDate: Map<LocalDate, DiaryEntry>,
    partnerByDate: Map<LocalDate, DiaryEntry>,
    episodesByDiaryEntryId: Map<String, List<Episode>>,
): List<RecapSlide.ClosestMoment> {
    val ownEpisodes = episodesOnDate(ownByDate, episodesByDiaryEntryId, date)
    val partnerEpisodes = episodesOnDate(partnerByDate, episodesByDiaryEntryId, date)
    return ownEpisodes.flatMap { own ->
        partnerEpisodes
            .filter { partner -> episodesOverlap(own, partner) }
            .map { partner -> closestMomentOf(date, own, partner) }
    }
}

private fun episodesOverlap(
    own: Episode,
    partner: Episode,
): Boolean = own.startTime <= partner.endTime && partner.startTime <= own.endTime

private fun closestMomentOf(
    date: LocalDate,
    own: Episode,
    partner: Episode,
): RecapSlide.ClosestMoment {
    val distance = haversineDistanceMeters(own.latitude, own.longitude, partner.latitude, partner.longitude)
    return RecapSlide.ClosestMoment(date, own, partner, distance)
}
