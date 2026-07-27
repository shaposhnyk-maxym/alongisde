package com.alongside.androidapp.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationManagerCompat

internal const val RECAP_NOTIFICATION_CHANNEL_ID = "recap_ready"

/**
 * Idempotent - safe to call on every process start, same contract as
 * [AndroidWorkManagerScheduler.ensurePeriodicSweepScheduled].
 */
internal fun createRecapNotificationChannel(context: Context) {
    val channel =
        NotificationChannel(
            RECAP_NOTIFICATION_CHANNEL_ID,
            "Recap ready",
            NotificationManager.IMPORTANCE_DEFAULT,
        )
    NotificationManagerCompat.from(context).createNotificationChannel(channel)
}
