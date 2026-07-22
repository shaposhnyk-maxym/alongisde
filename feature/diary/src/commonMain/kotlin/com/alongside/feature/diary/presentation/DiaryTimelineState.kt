package com.alongside.feature.diary.presentation

import androidx.compose.runtime.Immutable
import com.alongside.core.domain.diary.DayUnlockState
import com.alongside.core.domain.diary.DiaryDayLockReason
import com.alongside.core.domain.diary.buildDiaryTimelineDays
import com.alongside.core.domain.diary.resolveDayLockReason
import com.alongside.core.domain.diary.resolveDayUnlockState
import com.alongside.core.domain.trip.daysUntilReunion
import com.alongside.core.model.diary.DiaryEntry
import com.alongside.core.model.diary.Episode
import com.alongside.core.model.trip.Trip
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/**
 * Why a locked day is waiting, for display. Mirrors [DiaryDayLockReason] plus the one variant
 * that has no persisted signal - [GENERATING_TEXT] only exists while this device's own
 * [DiaryTimelineState.processingOwnDate] is set, driven live by the Container's in-flight call to
 * `EpisodeProcessingPipeline` (docs/roadmap.md M12: agreed with the user to wire this for real
 * rather than leave it preview-only, unlike M10/M11's still-unbuilt capture entry point).
 * [MISSED] (docs/roadmap.md M12.12) is permanent, unlike the other two "still waiting" variants -
 * it never resolves into UNLOCKED on its own.
 */
public enum class DiaryDayWaitingState {
    PARTNER_CAPTURING,
    WAITING_FOR_SYNC,
    GENERATING_TEXT,
    MISSED,
}

@Immutable
public data class DiaryDayCard(
    val date: LocalDate,
    val dayIndex: Int,
    val unlockState: DayUnlockState,
    val waitingState: DiaryDayWaitingState?,
    val ownEpisodes: List<Episode>,
    val partnerEpisodes: List<Episode>,
    val ownClosedAt: Instant?,
)

public sealed interface DiaryTimelineItem {
    public data class Countdown(
        val daysUntilReunion: Int,
    ) : DiaryTimelineItem

    public data class Day(
        val card: DiaryDayCard,
    ) : DiaryTimelineItem
}

@Immutable
public data class DiaryTimelineState(
    val today: LocalDate? = null,
    val ownUserId: String? = null,
    val partnerUserId: String? = null,
    val trip: Trip? = null,
    val ownEntries: List<DiaryEntry> = emptyList(),
    val partnerEntries: List<DiaryEntry> = emptyList(),
    val episodesByDiaryEntryId: Map<String, List<Episode>> = emptyMap(),
    val processingOwnDate: LocalDate? = null,
) {
    /** Derived, never stored, so the carousel can never drift out of sync with the raw data. */
    val items: List<DiaryTimelineItem>
        get() {
            val trip = trip ?: return emptyList()
            val today = today ?: return emptyList()
            val days =
                buildDiaryTimelineDays(
                    tripStartDate = trip.startDate,
                    tripEndDate = trip.endDate,
                    today = today,
                    ownEntries = ownEntries,
                    partnerEntries = partnerEntries,
                    episodesByDiaryEntryId = episodesByDiaryEntryId,
                )
            val dayItems =
                days.map { day ->
                    val unlockState = resolveDayUnlockState(day.ownStatus, day.partnerStatus)
                    val waitingState =
                        when {
                            unlockState == DayUnlockState.UNLOCKED -> null
                            day.date == processingOwnDate -> DiaryDayWaitingState.GENERATING_TEXT
                            else ->
                                when (resolveDayLockReason(day.ownStatus, day.partnerStatus)) {
                                    DiaryDayLockReason.MISSED -> DiaryDayWaitingState.MISSED
                                    DiaryDayLockReason.PARTNER_CAPTURING -> DiaryDayWaitingState.PARTNER_CAPTURING
                                    DiaryDayLockReason.WAITING_FOR_SYNC -> DiaryDayWaitingState.WAITING_FOR_SYNC
                                }
                        }
                    DiaryTimelineItem.Day(
                        DiaryDayCard(
                            date = day.date,
                            dayIndex = day.dayIndex,
                            unlockState = unlockState,
                            waitingState = waitingState,
                            ownEpisodes = day.ownEntry?.let { episodesByDiaryEntryId[it.id] }.orEmpty(),
                            partnerEpisodes = day.partnerEntry?.let { episodesByDiaryEntryId[it.id] }.orEmpty(),
                            ownClosedAt = day.ownEntry?.closedAt,
                        ),
                    )
                }
            val daysUntilReunion = daysUntilReunion(today, trip.startDate)
            // Once they've met, a "0 days to go" card is just noise - the carousel opens
            // straight on Day 1 instead.
            val countdownItem =
                if (daysUntilReunion > 0) listOf(DiaryTimelineItem.Countdown(daysUntilReunion)) else emptyList()
            return countdownItem + dayItems
        }
}
