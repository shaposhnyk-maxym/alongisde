package com.alongside.androidapp.work

import android.app.Application
import android.content.Context
import androidx.work.Configuration
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.alongside.core.domain.work.BackgroundJobKind
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

private const val PERIODIC_SWEEP_UNIQUE_NAME = "background-work-periodic-sweep"

// AlongsideApplication.onCreate() would otherwise start Koin for real during Robolectric's
// environment setup, entirely unrelated to what this test exercises - a plain stub Application
// avoids pulling in real Firebase/Koin wiring just to get a Context.
@Config(application = Application::class)
@RunWith(RobolectricTestRunner::class)
class AndroidWorkManagerSchedulerTest {
    private lateinit var workManager: WorkManager
    private lateinit var scheduler: AndroidWorkManagerScheduler

    @Before
    fun setUp() {
        val context: Context = RuntimeEnvironment.getApplication()
        val config = Configuration.Builder().setExecutor(SynchronousExecutor()).build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
        workManager = WorkManager.getInstance(context)
        scheduler = AndroidWorkManagerScheduler(context)
    }

    @Test
    fun `scheduleOneOff enqueues unique work constrained to a connected network`() {
        scheduler.scheduleOneOff(BackgroundJobKind.EPISODE_RETRY)

        val infos = workManager.getWorkInfosForUniqueWork(BackgroundJobKind.EPISODE_RETRY.name).get()
        val workInfo = infos.single()
        assertEquals(WorkInfo.State.ENQUEUED, workInfo.state)
        assertEquals(NetworkType.CONNECTED, workInfo.constraints.requiredNetworkType)
    }

    @Test
    fun `a second scheduleOneOff for the same kind while one is queued stays a single unique work`() {
        scheduler.scheduleOneOff(BackgroundJobKind.PLACE_RETRY)
        scheduler.scheduleOneOff(BackgroundJobKind.PLACE_RETRY)

        val infos = workManager.getWorkInfosForUniqueWork(BackgroundJobKind.PLACE_RETRY.name).get()
        assertEquals(1, infos.size)
    }

    @Test
    fun `ensurePeriodicSweepScheduled enqueues exactly one periodic request, idempotently`() {
        scheduler.ensurePeriodicSweepScheduled()
        scheduler.ensurePeriodicSweepScheduled()

        val infos = workManager.getWorkInfosForUniqueWork(PERIODIC_SWEEP_UNIQUE_NAME).get()
        assertEquals(1, infos.size)
        assertEquals(NetworkType.CONNECTED, infos.single().constraints.requiredNetworkType)
    }
}
