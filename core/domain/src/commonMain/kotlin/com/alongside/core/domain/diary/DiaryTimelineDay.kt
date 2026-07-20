package com.alongside.core.domain.diary

import com.alongside.core.model.diary.DiaryEntry
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
) {
    public val ownStatus: DiaryDayStatus get() = diaryDayStatus(ownEntry, today)
    public val partnerStatus: DiaryDayStatus get() = diaryDayStatus(partnerEntry, today)
}

/**
 * One [DiaryTimelineDay] per calendar day of the trip, [tripStartDate]..[tripEndDate] inclusive,
 * each independently matched to its own/partner [DiaryEntry] by date - so unlocking one day can
 * never affect another's computed status. [today] drives each day's auto-close-by-date-lapse
 * check (docs/roadmap.md M12.6) - a single value shared by every day in the trip, not
 * recomputed per day.
 */
public fun buildDiaryTimelineDays(
    tripStartDate: LocalDate,
    tripEndDate: LocalDate,
    today: LocalDate,
    ownEntries: List<DiaryEntry>,
    partnerEntries: List<DiaryEntry>,
): List<DiaryTimelineDay> {
    val ownByDate = ownEntries.associateBy { it.date }
    val partnerByDate = partnerEntries.associateBy { it.date }

    val days = mutableListOf<DiaryTimelineDay>()
    var date = tripStartDate
    var index = 1
    while (date <= tripEndDate) {
        days +=
            DiaryTimelineDay(
                date = date,
                dayIndex = index,
                today = today,
                ownEntry = ownByDate[date],
                partnerEntry = partnerByDate[date],
            )
        date = date.plus(1, DateTimeUnit.DAY)
        index++
    }
    return days
}
