package com.alongside.core.domain.diary

import com.alongside.core.model.SyncStatus
import com.alongside.core.model.diary.DiaryEntry
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

private val TODAY = LocalDate(2026, 7, 19)

class PartnerReadyPushTriggerTest {
    private fun entry(
        userId: String,
        syncStatus: SyncStatus,
        closedAt: Instant? = null,
    ) = DiaryEntry(
        id = "entry-$userId",
        tripId = "trip-1",
        userId = userId,
        date = TODAY,
        syncStatus = syncStatus,
        createdAt = Instant.fromEpochMilliseconds(0),
        updatedAt = Instant.fromEpochMilliseconds(0),
        closedAt = closedAt,
    )

    @Test
    fun `neither entry exists does not trigger`() {
        assertFalse(shouldTriggerPartnerReadyPush(own = null, partner = null, today = TODAY))
    }

    @Test
    fun `own missing does not trigger`() {
        assertFalse(
            shouldTriggerPartnerReadyPush(
                own = null,
                partner = entry("partner", SyncStatus.SYNCED, closedAt = Instant.fromEpochMilliseconds(1)),
                today = TODAY,
            ),
        )
    }

    @Test
    fun `partner missing does not trigger`() {
        assertFalse(
            shouldTriggerPartnerReadyPush(
                own = entry("own", SyncStatus.SYNCED, closedAt = Instant.fromEpochMilliseconds(1)),
                partner = null,
                today = TODAY,
            ),
        )
    }

    @Test
    fun `own pending does not trigger`() {
        assertFalse(
            shouldTriggerPartnerReadyPush(
                own = entry("own", SyncStatus.PENDING),
                partner = entry("partner", SyncStatus.SYNCED, closedAt = Instant.fromEpochMilliseconds(1)),
                today = TODAY,
            ),
        )
    }

    @Test
    fun `own syncing does not trigger`() {
        assertFalse(
            shouldTriggerPartnerReadyPush(
                own = entry("own", SyncStatus.SYNCING),
                partner = entry("partner", SyncStatus.SYNCED, closedAt = Instant.fromEpochMilliseconds(1)),
                today = TODAY,
            ),
        )
    }

    @Test
    fun `partner failed does not trigger`() {
        assertFalse(
            shouldTriggerPartnerReadyPush(
                own = entry("own", SyncStatus.SYNCED, closedAt = Instant.fromEpochMilliseconds(1)),
                partner = entry("partner", SyncStatus.FAILED),
                today = TODAY,
            ),
        )
    }

    @Test
    fun `both synced but neither closed does not trigger`() {
        assertFalse(
            shouldTriggerPartnerReadyPush(
                own = entry("own", SyncStatus.SYNCED),
                partner = entry("partner", SyncStatus.SYNCED),
                today = TODAY,
            ),
        )
    }

    @Test
    fun `both synced and closed triggers`() {
        assertTrue(
            shouldTriggerPartnerReadyPush(
                own = entry("own", SyncStatus.SYNCED, closedAt = Instant.fromEpochMilliseconds(1)),
                partner = entry("partner", SyncStatus.SYNCED, closedAt = Instant.fromEpochMilliseconds(2)),
                today = TODAY,
            ),
        )
    }

    @Test
    fun `both synced for a day that has already passed but has no episodes does not trigger`() {
        assertFalse(
            shouldTriggerPartnerReadyPush(
                own = entry("own", SyncStatus.SYNCED),
                partner = entry("partner", SyncStatus.SYNCED),
                today = TODAY.plus(1, DateTimeUnit.DAY),
            ),
        )
    }

    @Test
    fun `both synced for a day that has already passed with episodes on both sides triggers`() {
        assertTrue(
            shouldTriggerPartnerReadyPush(
                own = entry("own", SyncStatus.SYNCED),
                partner = entry("partner", SyncStatus.SYNCED),
                today = TODAY.plus(1, DateTimeUnit.DAY),
                ownHasEpisodes = true,
                partnerHasEpisodes = true,
            ),
        )
    }

    @Test
    fun `own side missing episodes for a passed day does not trigger even if partner has some`() {
        assertFalse(
            shouldTriggerPartnerReadyPush(
                own = entry("own", SyncStatus.SYNCED),
                partner = entry("partner", SyncStatus.SYNCED),
                today = TODAY.plus(1, DateTimeUnit.DAY),
                ownHasEpisodes = false,
                partnerHasEpisodes = true,
            ),
        )
    }
}
