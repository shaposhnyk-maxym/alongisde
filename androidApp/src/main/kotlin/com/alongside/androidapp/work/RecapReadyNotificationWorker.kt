package com.alongside.androidapp.work

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.alongside.androidapp.MainActivity
import com.alongside.core.domain.recap.RecapRepository
import kotlinx.coroutines.CancellationException
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

internal const val KEY_TRIP_ID = "trip_id"

/**
 * Fires the local "your recap is ready" notification (docs/roadmap.md M20.5) - scheduled far in
 * advance by [AndroidWorkManagerScheduler.scheduleRecapReadyNotification], so unlike
 * [BackgroundSyncWorker] a transient failure is worth WorkManager's bounded retry rather than
 * silently losing the one chance to notify.
 */
internal class RecapReadyNotificationWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params),
    KoinComponent {
    private val recapRepository: RecapRepository by inject()

    @Suppress("TooGenericExceptionCaught")
    override suspend fun doWork(): Result =
        try {
            val tripId = inputData.getString(KEY_TRIP_ID) ?: return Result.failure()
            // Defends the real "Delete Trip" case (feature:settings) - the trip/recap may no
            // longer exist by the time this fires, days after it was scheduled.
            recapRepository.getById(tripId) ?: return Result.success()
            if (NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()) {
                postNotification(tripId)
            }
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            println("RecapReadyNotificationWorker: failed, will retry: ${e::class.simpleName}: ${e.message}")
            Result.retry()
        }

    // The caller already gated on NotificationManagerCompat.areNotificationsEnabled() - lint's
    // static POST_NOTIFICATIONS check can't see that, but it's the same runtime guarantee.
    @SuppressLint("MissingPermission")
    private fun postNotification(tripId: String) {
        val openAppIntent =
            Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        val pendingIntent =
            PendingIntent.getActivity(
                applicationContext,
                tripId.hashCode(),
                openAppIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        val notification =
            NotificationCompat
                .Builder(applicationContext, RECAP_NOTIFICATION_CHANNEL_ID)
                // TODO(M20.5): swap for a dedicated app-branded icon once one exists - androidApp
                // has no res/ drawables at all yet, this system icon is a functional placeholder.
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Your recap is ready")
                .setContentText("You and your partner's trip recap is ready to view.")
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()
        NotificationManagerCompat.from(applicationContext).notify(tripId.hashCode(), notification)
    }
}
