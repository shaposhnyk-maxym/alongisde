package com.alongside.feature.diary.presentation

import androidx.lifecycle.ViewModel
import com.alongside.core.domain.auth.AuthSessionCache
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.container
import kotlin.time.Clock

public class DiaryTimelineContainer(
    private val authSessionCache: AuthSessionCache,
    private val timelineDataSource: DiaryTimelineDataSource,
    private val captureCoordinator: DiaryCaptureCoordinator,
    private val clock: Clock = Clock.System,
) : ViewModel(),
    ContainerHost<DiaryTimelineState, DiaryTimelineSideEffect> {
    override val container: Container<DiaryTimelineState, DiaryTimelineSideEffect> =
        container(DiaryTimelineState()) { observeTimeline() }

    public fun onIntent(intent: DiaryTimelineIntent) {
        when (intent) {
            is DiaryTimelineIntent.ProcessCapturedPhotos -> processCapturedPhotos(intent.date, intent.uris)
            is DiaryTimelineIntent.CloseDay -> closeDay(intent.date)
        }
    }

    private suspend fun Syntax<DiaryTimelineState, DiaryTimelineSideEffect>.observeTimeline() {
        val uid = authSessionCache.get()?.user?.uid ?: return
        val today = clock.todayIn(TimeZone.currentSystemDefault())
        reduce { state.copy(today = today, ownUserId = uid) }

        timelineDataSource.observe(uid) { (trip, entries, episodesByEntryId) ->
            val partnerUid = trip?.let { if (it.ownerId == uid) it.memberId else it.ownerId }
            reduce {
                state.copy(
                    trip = trip,
                    partnerUserId = partnerUid,
                    ownEntries = entries.filter { it.userId == uid },
                    partnerEntries = entries.filter { partnerUid != null && it.userId == partnerUid },
                    episodesByDiaryEntryId = episodesByEntryId,
                )
            }
        }
    }

    // date is whichever day is centered in the carousel, not necessarily today - restricting
    // capture to today/the trip's range is a deliberate follow-up (docs/roadmap.md M12.6).
    private fun processCapturedPhotos(
        date: LocalDate,
        uris: List<String>,
    ) = intent {
        val uid = state.ownUserId ?: authSessionCache.get()?.user?.uid ?: return@intent
        val trip = state.trip ?: return@intent

        reduce { state.copy(processingOwnDate = date) }

        captureCoordinator.capture(
            tripId = trip.id,
            userId = uid,
            date = date,
            existingEntryId = state.ownEntries.firstOrNull { it.date == date }?.id,
            uris = uris,
        )

        reduce { state.copy(processingOwnDate = null) }
    }

    private fun closeDay(date: LocalDate) =
        intent {
            val entry = state.ownEntries.firstOrNull { it.date == date } ?: return@intent
            captureCoordinator.closeDay(entry)
        }
}
