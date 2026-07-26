package com.alongside.core.domain.recap

import com.alongside.core.model.diary.DiaryEntry
import com.alongside.core.model.diary.Episode
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/** Every calendar date of the trip, inclusive, in order - shared by the day-based slide builders. */
internal fun tripDates(
    tripStartDate: LocalDate,
    tripEndDate: LocalDate,
): List<LocalDate> {
    val dates = mutableListOf<LocalDate>()
    var date = tripStartDate
    while (date <= tripEndDate) {
        dates += date
        date = date.plus(1, DateTimeUnit.DAY)
    }
    return dates
}

/** One side's episodes on [date], via that side's diary entry for the date, or empty if none. */
internal fun episodesOnDate(
    entriesByDate: Map<LocalDate, DiaryEntry>,
    episodesByDiaryEntryId: Map<String, List<Episode>>,
    date: LocalDate,
): List<Episode> = entriesByDate[date]?.let { episodesByDiaryEntryId[it.id] }.orEmpty()
