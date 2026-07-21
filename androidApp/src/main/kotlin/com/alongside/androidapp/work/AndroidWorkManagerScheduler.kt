package com.alongside.androidapp.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.alongside.core.domain.work.BackgroundJobKind
import com.alongside.core.domain.work.BackgroundWorkScheduler
import java.util.concurrent.TimeUnit

private const val PERIODIC_SWEEP_UNIQUE_NAME = "background-work-periodic-sweep"

// WorkManager's documented floor for PeriodicWorkRequest - a backstop interval, not the primary
// reliability path (that's the per-kind scheduleOneOff, fired immediately after the triggering
// action). Don't try to beat this floor with a chained one-off-with-delay - that's the same
// in-memory-poll antipattern this seam exists to replace, just moved inside WorkManager.
private const val PERIODIC_SWEEP_INTERVAL_MINUTES = 15L

private val CONNECTED_CONSTRAINTS: Constraints =
    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

/**
 * [BackgroundWorkScheduler] backed by [WorkManager]. Every enqueued request carries
 * `Constraints(NetworkType.CONNECTED)` - no point running any of these jobs without network.
 *
 * [scheduleOneOff] never places job-specific ids in the request's input data - [BackgroundSyncWorker]
 * always re-reads the full "needs retry" set from Room at run time, which is what makes
 * [ExistingWorkPolicy.KEEP] safe: a second `scheduleOneOff(kind)` call while the first is still
 * queued is a free no-op, since the eventual run recomputes everything anyway.
 */
internal class AndroidWorkManagerScheduler(
    private val context: Context,
) : BackgroundWorkScheduler {
    override fun scheduleOneOff(kind: BackgroundJobKind) {
        val request =
            OneTimeWorkRequestBuilder<BackgroundSyncWorker>()
                .setInputData(Data.Builder().putString(KEY_JOB_KIND, kind.name).build())
                .setConstraints(CONNECTED_CONSTRAINTS)
                .build()
        WorkManager.getInstance(context).enqueueUniqueWork(kind.name, ExistingWorkPolicy.KEEP, request)
    }

    override fun ensurePeriodicSweepScheduled() {
        val request =
            PeriodicWorkRequestBuilder<BackgroundSyncWorker>(PERIODIC_SWEEP_INTERVAL_MINUTES, TimeUnit.MINUTES)
                .setConstraints(CONNECTED_CONSTRAINTS)
                .build()
        WorkManager
            .getInstance(context)
            .enqueueUniquePeriodicWork(PERIODIC_SWEEP_UNIQUE_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }
}
