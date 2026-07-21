package com.alongside.androidapp.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.alongside.core.domain.auth.AuthSessionCache
import com.alongside.core.domain.work.BackgroundJobKind
import com.alongside.data.sync.SyncCoordinator
import com.alongside.feature.diary.presentation.DiaryCaptureCoordinator
import com.alongside.feature.places.presentation.PlaceRetryCoordinator
import kotlinx.coroutines.CancellationException
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

internal const val KEY_JOB_KIND = "job_kind"

/**
 * Single WorkManager entry point for all background-work reliability jobs (docs/roadmap.md
 * M12.11) - dispatches by [BackgroundJobKind] when [KEY_JOB_KIND] is present in [getInputData]
 * (the fast, event-driven one-off path); when absent (the periodic sweep, see
 * [AndroidWorkManagerScheduler.ensurePeriodicSweepScheduled]), runs every kind in one pass.
 *
 * `by inject()` (not a custom `WorkerFactory`/`Configuration.Provider`) resolves lazily inside
 * [doWork], well after Koin has started in `Application.onCreate()` - a custom factory would
 * itself be constructed by WorkManager's own `androidx.startup` initializer, which runs *before*
 * `Application.onCreate()`, a real init-order hazard `by inject()` sidesteps entirely.
 *
 * Best-effort, not per-item retry: returns [Result.success] even when some items are still
 * incomplete - photo uploads are uncapped by design (see `DiaryCaptureCoordinator`), and the
 * periodic sweep plus future event-driven enqueues pick up whatever's left. [Result.retry] is
 * reserved for a hard failure of the *whole* one-off pass (e.g. resolving the current user
 * throws) - never ordinary per-item incompleteness, which would otherwise fight with
 * `descriptionAttempts`' own cap and could eventually strand an uncapped photo retry under
 * WorkManager's own max-attempt limit.
 */
internal class BackgroundSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params),
    KoinComponent {
    private val authSessionCache: AuthSessionCache by inject()
    private val diaryCaptureCoordinator: DiaryCaptureCoordinator by inject()
    private val placeRetryCoordinator: PlaceRetryCoordinator by inject()
    private val syncCoordinator: SyncCoordinator by inject()

    @Suppress("TooGenericExceptionCaught")
    override suspend fun doWork(): Result =
        try {
            val kindName = inputData.getString(KEY_JOB_KIND)
            if (kindName == null) {
                BackgroundJobKind.entries.forEach { kind -> runJobCatching(kind) }
            } else {
                runJob(BackgroundJobKind.valueOf(kindName))
            }
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            println("BackgroundSyncWorker: one-off job failed, will retry: ${e::class.simpleName}: ${e.message}")
            Result.retry()
        }

    // The periodic sweep gathers every kind in one pass - one kind's hard failure shouldn't
    // block the others, and the sweep itself always succeeds (there's nothing to retry sooner
    // than its own next scheduled run anyway).
    @Suppress("TooGenericExceptionCaught")
    private suspend fun runJobCatching(kind: BackgroundJobKind) {
        try {
            runJob(kind)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            println("BackgroundSyncWorker: $kind failed during periodic sweep: $e")
        }
    }

    private suspend fun runJob(kind: BackgroundJobKind) {
        val ownUserId = authSessionCache.get()?.user?.uid ?: return
        when (kind) {
            BackgroundJobKind.EPISODE_RETRY -> diaryCaptureCoordinator.retryAllIncompleteEpisodes(ownUserId)
            BackgroundJobKind.PLACE_RETRY -> placeRetryCoordinator.retryAllIncompletePlaces(ownUserId)
            BackgroundJobKind.SYNC_QUEUE_FLUSH -> syncCoordinator.sync()
        }
    }
}
