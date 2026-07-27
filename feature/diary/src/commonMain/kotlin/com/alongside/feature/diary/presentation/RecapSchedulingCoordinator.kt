package com.alongside.feature.diary.presentation

import com.alongside.core.domain.diary.DiaryDayStatus
import com.alongside.core.domain.diary.shouldScheduleRecap
import com.alongside.core.domain.recap.RecapRepository
import com.alongside.core.domain.recap.recapAvailableAt
import com.alongside.core.domain.work.BackgroundWorkScheduler
import kotlinx.datetime.LocalDate

/**
 * The write step `resolveDayUnlockState`'s `recapScheduled` param assumes exists (docs/roadmap.md
 * M20.1) - mirrors `PlaceContentPullCoordinator`'s shape. Also schedules the local "recap ready"
 * notification (docs/roadmap.md M20.5) at the same trigger - one moment, two idempotent side
 * effects, not two separate mechanisms.
 */
public class RecapSchedulingCoordinator(
    private val recapRepository: RecapRepository,
    private val backgroundWorkScheduler: BackgroundWorkScheduler,
) {
    public suspend fun ensureScheduledIfReady(
        tripId: String,
        date: LocalDate,
        tripEndDate: LocalDate,
        own: DiaryDayStatus,
        partner: DiaryDayStatus,
    ) {
        if (!shouldScheduleRecap(date, tripEndDate, own, partner)) return
        val availableAt = recapAvailableAt(tripEndDate)
        recapRepository.ensureScheduled(tripId, availableAt)
        backgroundWorkScheduler.scheduleRecapReadyNotification(tripId, availableAt)
    }
}
