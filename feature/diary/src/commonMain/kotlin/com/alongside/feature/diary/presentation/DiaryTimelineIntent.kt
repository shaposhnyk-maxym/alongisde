package com.alongside.feature.diary.presentation

import kotlinx.datetime.LocalDate

public sealed interface DiaryTimelineIntent {
    /**
     * Runs capture->processing (M10's pipeline) for [date]'s photos and persists the result.
     * [date] is whichever day is currently centered in the Timeline carousel, not necessarily
     * today - restricting capture to the trip's date range/today only is a deliberate follow-up
     * (docs/roadmap.md M12.6), not enforced here yet so any day is capturable for now.
     */
    public data class ProcessCapturedPhotos(
        val date: LocalDate,
        val uris: List<String>,
    ) : DiaryTimelineIntent

    /** Marks [date]'s own entry closed - final, no more photos can be added to it afterward. */
    public data class CloseDay(
        val date: LocalDate,
    ) : DiaryTimelineIntent
}
