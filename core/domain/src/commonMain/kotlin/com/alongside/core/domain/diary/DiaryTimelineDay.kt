package com.alongside.core.domain.diary

import com.alongside.core.model.diary.DiaryEntry
import com.alongside.core.model.diary.Episode
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/** One day-card slot in the Timeline carousel (docs/roadmap.md M12), one-based for display. */
public data class DiaryTimelineDay(
    val date: LocalDate,
    val dayIndex: Int,
    val today: LocalDate,
    val ownEntry: DiaryEntry?,
    val partnerEntry: DiaryEntry?,
    val ownHasEpisodes: Boolean = false,
    val partnerHasEpisodes: Boolean = false,
) {
    public val ownStatus: DiaryDayStatus
        get() = diaryDayStatus(ownEntry, date, today, ownHasEpisodes)
    public val partnerStatus: DiaryDayStatus
        get() = diaryDayStatus(partnerEntry, date, today, partnerHasEpisodes)
}

/**
 * One [DiaryTimelineDay] per calendar day of the trip, [tripStartDate]..[tripEndDate] inclusive,
 * each independently matched to its own/partner [DiaryEntry] by date - so unlocking one day can
 * never affect another's computed status. [today] drives each day's auto-close-by-date-lapse
 * check (docs/roadmap.md M12.6) - a single value shared by every day in the trip, not
 * recomputed per day. [episodesByDiaryEntryId] (docs/roadmap.md M12.12) drives whether a bygone
 * day is READY or MISSED - defaults to empty so callers that don't care about MISSED (most
 * existing tests) don't need to thread it through.
 */
public fun buildDiaryTimelineDays(
    tripStartDate: LocalDate,
    tripEndDate: LocalDate,
    today: LocalDate,
    ownEntries: List<DiaryEntry>,
    partnerEntries: List<DiaryEntry>,
    episodesByDiaryEntryId: Map<String, List<Episode>> = emptyMap(),
): List<DiaryTimelineDay> {
    val ownByDate = ownEntries.associateBy { it.date }
    val partnerByDate = partnerEntries.associateBy { it.date }

    val days = mutableListOf<DiaryTimelineDay>()
    var date = tripStartDate
    var index = 1
    while (date <= tripEndDate) {
        val ownEntry = ownByDate[date]
        val partnerEntry = partnerByDate[date]
        days +=
            DiaryTimelineDay(
                date = date,
                dayIndex = index,
                today = today,
                ownEntry = ownEntry,
                partnerEntry = partnerEntry,
                ownHasEpisodes = ownEntry?.let { episodesByDiaryEntryId[it.id] }?.isNotEmpty() ?: false,
                partnerHasEpisodes = partnerEntry?.let { episodesByDiaryEntryId[it.id] }?.isNotEmpty() ?: false,
            )
        date = date.plus(1, DateTimeUnit.DAY)
        index++
    }
    return days
}
