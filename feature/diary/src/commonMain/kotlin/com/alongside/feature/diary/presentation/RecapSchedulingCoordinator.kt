package com.alongside.feature.diary.presentation

import com.alongside.core.domain.diary.DiaryDayStatus
import com.alongside.core.domain.diary.shouldScheduleRecap
import com.alongside.core.domain.recap.RecapRepository
import com.alongside.core.domain.recap.recapAvailableAt
import kotlinx.datetime.LocalDate

/**
 * The write step `resolveDayUnlockState`'s `recapScheduled` param assumes exists (docs/roadmap.md
 * M20.1) - mirrors `PlaceContentPullCoordinator`'s shape.
 */
public class RecapSchedulingCoordinator(
    private val recapRepository: RecapRepository,
) {
    public suspend fun ensureScheduledIfReady(
        tripId: String,
        date: LocalDate,
        tripEndDate: LocalDate,
        own: DiaryDayStatus,
        partner: DiaryDayStatus,
    ) {
        if (!shouldScheduleRecap(date, tripEndDate, own, partner)) return
        recapRepository.ensureScheduled(tripId, recapAvailableAt(tripEndDate))
    }
}
