package com.alongside.androidapp.work

import android.app.Application
import android.app.NotificationManager
import androidx.core.content.getSystemService
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.alongside.androidapp.MainActivity
import com.alongside.core.domain.recap.RecapRepository
import com.alongside.core.model.recap.Recap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

private class FakeRecapRepository(
    private val recap: Recap?,
) : RecapRepository {
    override suspend fun ensureScheduled(
        tripId: String,
        availableAt: LocalDate,
    ) = error("not used by this test")

    override suspend fun getById(tripId: String): Recap? = recap

    override fun observeById(tripId: String): Flow<Recap?> = flowOf(recap)
}

// Same reasoning as BackgroundSyncWorkerTest: a stub Application avoids
// AlongsideApplication.onCreate() starting real Koin/Firebase wiring during Robolectric's own
// environment setup, before this test's @Before ever runs.
@Config(application = Application::class)
@RunWith(RobolectricTestRunner::class)
class RecapReadyNotificationWorkerTest {
    private val notificationManager
        get() = RuntimeEnvironment.getApplication().getSystemService<NotificationManager>()!!

    private fun restartKoinWith(recap: Recap?) {
        stopKoin()
        startKoin {
            modules(module { single<RecapRepository> { FakeRecapRepository(recap) } })
        }
    }

    @Before
    fun setUp() {
        stopKoin()
        createRecapNotificationChannel(RuntimeEnvironment.getApplication())
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    private fun worker(inputData: Data): RecapReadyNotificationWorker =
        TestListenableWorkerBuilder<RecapReadyNotificationWorker>(RuntimeEnvironment.getApplication())
            .setInputData(inputData)
            .build()

    @Test
    fun `posts a notification when the recap still exists and notifications are enabled`() =
        runBlocking {
            restartKoinWith(Recap(tripId = "trip-1", availableAt = LocalDate(2026, 7, 20)))
            shadowOf(notificationManager).setNotificationsEnabled(true)

            val result = worker(workDataOf(KEY_TRIP_ID to "trip-1")).doWork()

            assertEquals(ListenableWorker.Result.success(), result)
            val posted = shadowOf(notificationManager).getNotification("trip-1".hashCode())
            assertEquals("Your recap is ready", posted.extras.getString("android.title"))
            val savedIntent = shadowOf(posted.contentIntent).savedIntent
            assertEquals(MainActivity::class.java.name, savedIntent.component?.className)
        }

    @Test
    fun `posts nothing when the recap row no longer exists (deleted trip)`() =
        runBlocking {
            restartKoinWith(recap = null)
            shadowOf(notificationManager).setNotificationsEnabled(true)

            val result = worker(workDataOf(KEY_TRIP_ID to "trip-1")).doWork()

            assertEquals(ListenableWorker.Result.success(), result)
            assertEquals(0, shadowOf(notificationManager).size())
        }

    @Test
    fun `posts nothing when notifications are disabled`() =
        runBlocking {
            restartKoinWith(Recap(tripId = "trip-1", availableAt = LocalDate(2026, 7, 20)))
            shadowOf(notificationManager).setNotificationsEnabled(false)

            val result = worker(workDataOf(KEY_TRIP_ID to "trip-1")).doWork()

            assertEquals(ListenableWorker.Result.success(), result)
            assertEquals(0, shadowOf(notificationManager).size())
        }

    @Test
    fun `missing trip id fails without touching the repository`() =
        runBlocking {
            restartKoinWith(recap = null)

            val result = worker(Data.EMPTY).doWork()

            assertEquals(ListenableWorker.Result.failure(), result)
            assertNull(shadowOf(notificationManager).getNotification("trip-1".hashCode()))
        }
}
