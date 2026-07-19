package com.alongside.core.domain.diary

import com.alongside.core.model.SyncStatus
import com.alongside.core.model.diary.DiaryEntry
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

class PartnerReadyPushTriggerTest {
    private fun entry(
        userId: String,
        syncStatus: SyncStatus,
    ) = DiaryEntry(
        id = "entry-$userId",
        tripId = "trip-1",
        userId = userId,
        date = LocalDate(2026, 7, 19),
        syncStatus = syncStatus,
        createdAt = Instant.fromEpochMilliseconds(0),
        updatedAt = Instant.fromEpochMilliseconds(0),
    )

    @Test
    fun `neither entry exists does not trigger`() {
        assertFalse(shouldTriggerPartnerReadyPush(own = null, partner = null))
    }

    @Test
    fun `own missing does not trigger`() {
        assertFalse(
            shouldTriggerPartnerReadyPush(own = null, partner = entry("partner", SyncStatus.SYNCED)),
        )
    }

    @Test
    fun `partner missing does not trigger`() {
        assertFalse(
            shouldTriggerPartnerReadyPush(own = entry("own", SyncStatus.SYNCED), partner = null),
        )
    }

    @Test
    fun `own pending does not trigger`() {
        assertFalse(
            shouldTriggerPartnerReadyPush(
                own = entry("own", SyncStatus.PENDING),
                partner = entry("partner", SyncStatus.SYNCED),
            ),
        )
    }

    @Test
    fun `own syncing does not trigger`() {
        assertFalse(
            shouldTriggerPartnerReadyPush(
                own = entry("own", SyncStatus.SYNCING),
                partner = entry("partner", SyncStatus.SYNCED),
            ),
        )
    }

    @Test
    fun `partner failed does not trigger`() {
        assertFalse(
            shouldTriggerPartnerReadyPush(
                own = entry("own", SyncStatus.SYNCED),
                partner = entry("partner", SyncStatus.FAILED),
            ),
        )
    }

    @Test
    fun `both synced triggers`() {
        assertTrue(
            shouldTriggerPartnerReadyPush(
                own = entry("own", SyncStatus.SYNCED),
                partner = entry("partner", SyncStatus.SYNCED),
            ),
        )
    }
}
