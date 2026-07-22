package com.alongside.core.domain.diary

import com.alongside.core.model.SyncStatus
import com.alongside.core.model.diary.DiaryEntry
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class DayUnlockStateTest {
    @Test
    fun `both ready unlocks the day`() {
        assertEquals(
            DayUnlockState.UNLOCKED,
            resolveDayUnlockState(own = DiaryDayStatus.READY, partner = DiaryDayStatus.READY),
        )
    }

    @Test
    fun `neither ready stays locked`() {
        assertEquals(
            DayUnlockState.LOCKED,
            resolveDayUnlockState(own = DiaryDayStatus.NOT_READY, partner = DiaryDayStatus.NOT_READY),
        )
    }

    @Test
    fun `only own ready stays locked`() {
        assertEquals(
            DayUnlockState.LOCKED,
            resolveDayUnlockState(own = DiaryDayStatus.READY, partner = DiaryDayStatus.NOT_READY),
        )
    }

    @Test
    fun `only partner ready stays locked`() {
        assertEquals(
            DayUnlockState.LOCKED,
            resolveDayUnlockState(own = DiaryDayStatus.NOT_READY, partner = DiaryDayStatus.READY),
        )
    }

    @Test
    fun `own pending sync while partner not ready stays locked`() {
        assertEquals(
            DayUnlockState.LOCKED,
            resolveDayUnlockState(own = DiaryDayStatus.PENDING_SYNC, partner = DiaryDayStatus.NOT_READY),
        )
    }

    @Test
    fun `own pending sync while partner ready stays locked`() {
        assertEquals(
            DayUnlockState.LOCKED,
            resolveDayUnlockState(own = DiaryDayStatus.PENDING_SYNC, partner = DiaryDayStatus.READY),
        )
    }

    @Test
    fun `partner pending sync while own not ready stays locked`() {
        assertEquals(
            DayUnlockState.LOCKED,
            resolveDayUnlockState(own = DiaryDayStatus.NOT_READY, partner = DiaryDayStatus.PENDING_SYNC),
        )
    }

    @Test
    fun `partner pending sync while own ready stays locked`() {
        assertEquals(
            DayUnlockState.LOCKED,
            resolveDayUnlockState(own = DiaryDayStatus.READY, partner = DiaryDayStatus.PENDING_SYNC),
        )
    }

    @Test
    fun `both pending sync stays locked`() {
        assertEquals(
            DayUnlockState.LOCKED,
            resolveDayUnlockState(own = DiaryDayStatus.PENDING_SYNC, partner = DiaryDayStatus.PENDING_SYNC),
        )
    }

    @Test
    fun `own missed stays locked even if partner is ready`() {
        assertEquals(
            DayUnlockState.LOCKED,
            resolveDayUnlockState(own = DiaryDayStatus.MISSED, partner = DiaryDayStatus.READY),
        )
    }

    @Test
    fun `partner missed stays locked even if own is ready`() {
        assertEquals(
            DayUnlockState.LOCKED,
            resolveDayUnlockState(own = DiaryDayStatus.READY, partner = DiaryDayStatus.MISSED),
        )
    }
}

class DiaryDayStatusTest {
    private val today = LocalDate(2026, 7, 19)
    private val fixture =
        DiaryEntry(
            id = "entry-1",
            tripId = "trip-1",
            userId = "user-1",
            date = today,
            syncStatus = SyncStatus.PENDING,
            createdAt = Instant.fromEpochMilliseconds(0),
            updatedAt = Instant.fromEpochMilliseconds(0),
        )

    @Test
    fun `null entry for today or the future is not ready`() {
        assertEquals(DiaryDayStatus.NOT_READY, diaryDayStatus(null, date = today, today = today, hasEpisodes = false))
    }

    @Test
    fun `null entry whose day has already passed is missed`() {
        val date = today
        val laterToday = today.plus(1, DateTimeUnit.DAY)
        assertEquals(DiaryDayStatus.MISSED, diaryDayStatus(null, date = date, today = laterToday, hasEpisodes = false))
    }

    @Test
    fun `pending entry is pending sync`() {
        val pending = fixture.copy(syncStatus = SyncStatus.PENDING)
        assertEquals(
            DiaryDayStatus.PENDING_SYNC,
            diaryDayStatus(pending, date = today, today = today, hasEpisodes = false),
        )
    }

    @Test
    fun `syncing entry is pending sync`() {
        val syncing = fixture.copy(syncStatus = SyncStatus.SYNCING)
        assertEquals(
            DiaryDayStatus.PENDING_SYNC,
            diaryDayStatus(syncing, date = today, today = today, hasEpisodes = false),
        )
    }

    @Test
    fun `failed entry is pending sync`() {
        val failed = fixture.copy(syncStatus = SyncStatus.FAILED)
        assertEquals(
            DiaryDayStatus.PENDING_SYNC,
            diaryDayStatus(failed, date = today, today = today, hasEpisodes = false),
        )
    }

    @Test
    fun `synced entry for today that is not closed is open`() {
        val synced = fixture.copy(syncStatus = SyncStatus.SYNCED)
        assertEquals(DiaryDayStatus.OPEN, diaryDayStatus(synced, date = today, today = today, hasEpisodes = false))
    }

    @Test
    fun `synced entry that is explicitly closed is ready`() {
        val closed = fixture.copy(syncStatus = SyncStatus.SYNCED, closedAt = Instant.fromEpochMilliseconds(1))
        assertEquals(DiaryDayStatus.READY, diaryDayStatus(closed, date = today, today = today, hasEpisodes = false))
    }

    @Test
    fun `synced entry that is explicitly closed is ready even if its day has passed with no episodes`() {
        val closed = fixture.copy(syncStatus = SyncStatus.SYNCED, closedAt = Instant.fromEpochMilliseconds(1))
        val laterToday = today.plus(1, DateTimeUnit.DAY)
        assertEquals(
            DiaryDayStatus.READY,
            diaryDayStatus(closed, date = today, today = laterToday, hasEpisodes = false),
        )
    }

    @Test
    fun `synced entry whose date has already passed with episodes is ready without an explicit close`() {
        val yesterday = fixture.copy(syncStatus = SyncStatus.SYNCED)
        val laterToday = today.plus(1, DateTimeUnit.DAY)
        assertEquals(
            DiaryDayStatus.READY,
            diaryDayStatus(yesterday, date = today, today = laterToday, hasEpisodes = true),
        )
    }

    @Test
    fun `synced entry whose date has already passed with no episodes is missed`() {
        val yesterday = fixture.copy(syncStatus = SyncStatus.SYNCED)
        val laterToday = today.plus(1, DateTimeUnit.DAY)
        assertEquals(
            DiaryDayStatus.MISSED,
            diaryDayStatus(yesterday, date = today, today = laterToday, hasEpisodes = false),
        )
    }
}
