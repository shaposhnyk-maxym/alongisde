package com.alongside.core.domain.diary

import com.alongside.core.model.SyncStatus
import com.alongside.core.model.diary.DiaryEntry
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

private fun entry(
    id: String,
    userId: String,
    date: LocalDate,
    syncStatus: SyncStatus = SyncStatus.SYNCED,
    closedAt: Instant? = null,
) = DiaryEntry(
    id = id,
    tripId = "trip-1",
    userId = userId,
    date = date,
    syncStatus = syncStatus,
    createdAt = Instant.fromEpochMilliseconds(0),
    updatedAt = Instant.fromEpochMilliseconds(0),
    closedAt = closedAt,
)

class DiaryTimelineDayTest {
    @Test
    fun `a single-day trip yields exactly one day at index 1`() {
        val days =
            buildDiaryTimelineDays(
                tripStartDate = LocalDate(2026, 7, 18),
                tripEndDate = LocalDate(2026, 7, 18),
                today = LocalDate(2026, 7, 18),
                ownEntries = emptyList(),
                partnerEntries = emptyList(),
            )

        assertEquals(1, days.size)
        assertEquals(LocalDate(2026, 7, 18), days.single().date)
        assertEquals(1, days.single().dayIndex)
    }

    @Test
    fun `days are ordered by date with sequential one-based indices`() {
        val days =
            buildDiaryTimelineDays(
                tripStartDate = LocalDate(2026, 7, 18),
                tripEndDate = LocalDate(2026, 7, 21),
                today = LocalDate(2026, 7, 21),
                ownEntries = emptyList(),
                partnerEntries = emptyList(),
            )

        assertEquals(
            listOf(LocalDate(2026, 7, 18), LocalDate(2026, 7, 19), LocalDate(2026, 7, 20), LocalDate(2026, 7, 21)),
            days.map { it.date },
        )
        assertEquals(listOf(1, 2, 3, 4), days.map { it.dayIndex })
    }

    @Test
    fun `a day with no entry on either side has null own and partner entries`() {
        val days =
            buildDiaryTimelineDays(
                tripStartDate = LocalDate(2026, 7, 18),
                tripEndDate = LocalDate(2026, 7, 18),
                today = LocalDate(2026, 7, 18),
                ownEntries = emptyList(),
                partnerEntries = emptyList(),
            )

        val day = days.single()
        assertNull(day.ownEntry)
        assertNull(day.partnerEntry)
        assertEquals(DiaryDayStatus.NOT_READY, day.ownStatus)
        assertEquals(DiaryDayStatus.NOT_READY, day.partnerStatus)
    }

    @Test
    fun `a synced entry for today that is not closed yet is OPEN, not READY`() {
        val date = LocalDate(2026, 7, 19)
        val ownEntry = entry(id = "own-1", userId = "own", date = date, syncStatus = SyncStatus.SYNCED)

        val days =
            buildDiaryTimelineDays(
                tripStartDate = LocalDate(2026, 7, 18),
                tripEndDate = LocalDate(2026, 7, 20),
                today = date,
                ownEntries = listOf(ownEntry),
                partnerEntries = emptyList(),
            )

        val day = days.single { it.date == date }
        assertEquals(ownEntry, day.ownEntry)
        assertNull(day.partnerEntry)
        assertEquals(DiaryDayStatus.OPEN, day.ownStatus)
        assertEquals(DiaryDayStatus.NOT_READY, day.partnerStatus)

        val otherDays = days.filter { it.date != date }
        assertEquals(listOf(null, null), otherDays.map { it.ownEntry })
    }

    @Test
    fun `an explicitly closed entry for today is READY`() {
        val date = LocalDate(2026, 7, 19)
        val ownEntry =
            entry(
                id = "own-1",
                userId = "own",
                date = date,
                syncStatus = SyncStatus.SYNCED,
                closedAt = Instant.fromEpochMilliseconds(1_000),
            )

        val days =
            buildDiaryTimelineDays(
                tripStartDate = date,
                tripEndDate = date,
                today = date,
                ownEntries = listOf(ownEntry),
                partnerEntries = emptyList(),
            )

        assertEquals(DiaryDayStatus.READY, days.single().ownStatus)
    }

    @Test
    fun `a synced entry whose date has already passed is READY even without an explicit close`() {
        val date = LocalDate(2026, 7, 19)
        val ownEntry = entry(id = "own-1", userId = "own", date = date, syncStatus = SyncStatus.SYNCED)

        val days =
            buildDiaryTimelineDays(
                tripStartDate = date,
                tripEndDate = date,
                today = LocalDate(2026, 7, 20),
                ownEntries = listOf(ownEntry),
                partnerEntries = emptyList(),
            )

        assertEquals(DiaryDayStatus.READY, days.single().ownStatus)
    }

    @Test
    fun `both sides captured for the same date are matched independently`() {
        val date = LocalDate(2026, 7, 19)
        val ownEntry = entry(id = "own-1", userId = "own", date = date, syncStatus = SyncStatus.PENDING)
        val partnerEntry = entry(id = "partner-1", userId = "partner", date = date, syncStatus = SyncStatus.SYNCED)

        val days =
            buildDiaryTimelineDays(
                tripStartDate = date,
                tripEndDate = date,
                today = date,
                ownEntries = listOf(ownEntry),
                partnerEntries = listOf(partnerEntry),
            )

        val day = days.single()
        assertEquals(ownEntry, day.ownEntry)
        assertEquals(partnerEntry, day.partnerEntry)
        assertEquals(DiaryDayStatus.PENDING_SYNC, day.ownStatus)
        assertEquals(DiaryDayStatus.OPEN, day.partnerStatus)
    }
}
