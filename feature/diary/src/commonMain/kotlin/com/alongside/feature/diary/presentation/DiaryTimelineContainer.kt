package com.alongside.feature.diary.presentation

import androidx.lifecycle.ViewModel
import com.alongside.core.domain.auth.AuthSessionCache
import com.alongside.core.domain.diary.diaryDayStatus
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
    private val preTripPhotoCaptureCoordinator: PreTripPhotoCaptureCoordinator,
    private val recapSchedulingCoordinator: RecapSchedulingCoordinator,
    private val clock: Clock = Clock.System,
) : ViewModel(),
    ContainerHost<DiaryTimelineState, DiaryTimelineSideEffect> {
    override val container: Container<DiaryTimelineState, DiaryTimelineSideEffect> =
        container(DiaryTimelineState()) { observeTimeline() }

    public fun onIntent(intent: DiaryTimelineIntent) {
        when (intent) {
            is DiaryTimelineIntent.ProcessCapturedPhotos -> processCapturedPhotos(intent.date, intent.uris)
            is DiaryTimelineIntent.CloseDay -> closeDay(intent.date)
            is DiaryTimelineIntent.ProcessPreTripPhotos -> processPreTripPhotos(intent.uris)
            DiaryTimelineIntent.FlushPreTripPhotoSync -> preTripPhotoCaptureCoordinator.flushPendingSync()
        }
    }

    private suspend fun Syntax<DiaryTimelineState, DiaryTimelineSideEffect>.observeTimeline() {
        val uid = authSessionCache.get()?.user?.uid ?: return
        val today = clock.todayIn(TimeZone.currentSystemDefault())
        reduce { state.copy(today = today, ownUserId = uid) }

        timelineDataSource.observe(uid) { snapshot ->
            val partnerUid = snapshot.trip?.let { if (it.ownerId == uid) it.memberId else it.ownerId }
            reduce {
                state.copy(
                    trip = snapshot.trip,
                    partnerUserId = partnerUid,
                    ownEntries = snapshot.entries.filter { it.userId == uid },
                    partnerEntries = snapshot.entries.filter { partnerUid != null && it.userId == partnerUid },
                    episodesByDiaryEntryId = snapshot.episodesByDiaryEntryId,
                    ownPreTripPhotos = snapshot.ownPreTripPhotos,
                    partnerPreTripPhotos = snapshot.partnerPreTripPhotos,
                    recap = snapshot.recap,
                )
            }
            maybeScheduleRecap(uid, partnerUid, today, snapshot)
        }
    }

    private suspend fun maybeScheduleRecap(
        uid: String,
        partnerUid: String?,
        today: LocalDate,
        snapshot: TimelineSnapshot,
    ) {
        val trip = snapshot.trip ?: return
        if (snapshot.recap != null) return // cheap short-circuit; ensureScheduled is idempotent regardless
        val ownEntry = snapshot.entries.firstOrNull { it.userId == uid && it.date == trip.endDate }
        val partnerEntry =
            partnerUid?.let { p -> snapshot.entries.firstOrNull { it.userId == p && it.date == trip.endDate } }
        val ownHasEpisodes = ownEntry?.let { snapshot.episodesByDiaryEntryId[it.id] }?.isNotEmpty() ?: false
        val partnerHasEpisodes = partnerEntry?.let { snapshot.episodesByDiaryEntryId[it.id] }?.isNotEmpty() ?: false
        recapSchedulingCoordinator.ensureScheduledIfReady(
            tripId = trip.id,
            date = trip.endDate,
            tripEndDate = trip.endDate,
            own = diaryDayStatus(ownEntry, trip.endDate, today, ownHasEpisodes),
            partner = diaryDayStatus(partnerEntry, trip.endDate, today, partnerHasEpisodes),
        )
    }

    // date is whichever day is centered in the carousel, not necessarily today - the UI already
    // hides "Add Photos" once that date has passed (docs/roadmap.md M12.12), and
    // DiaryCaptureCoordinator.capture() defensively no-ops for a past date too, in case some
    // other entry point ever reaches this without going through that UI gate. Restricting to the
    // trip's own date range (can't backdate before it started) remains a follow-up.
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

    private fun processPreTripPhotos(uris: List<String>) =
        intent {
            val uid = state.ownUserId ?: authSessionCache.get()?.user?.uid ?: return@intent
            val trip = state.trip ?: return@intent
            preTripPhotoCaptureCoordinator.capture(trip.id, uid, uris)
        }
}
