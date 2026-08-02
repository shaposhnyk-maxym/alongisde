package com.alongside.app.work

import com.alongside.core.domain.recap.durationUntilRecapNotification
import com.alongside.core.domain.work.BackgroundJobKind
import com.alongside.core.domain.work.BackgroundWorkScheduler
import kotlinx.datetime.LocalDate
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.time.Clock

// UNTimeIntervalNotificationTrigger throws NSInvalidArgumentException for a zero/negative
// interval - durationUntilRecapNotification itself only clamps at zero (already-passed dates
// are a legitimate input, e.g. a delayed app launch after the recap date), so the floor is
// applied here instead, at the platform boundary that actually needs it.
private const val MIN_TRIGGER_INTERVAL_SECONDS = 1.0

/**
 * Real [BackgroundWorkScheduler] for iOS, but only for [scheduleRecapReadyNotification]
 * (docs/roadmap.md M21.3) - replaces `NoOpBackgroundWorkScheduler` for that one method only.
 * [scheduleOneOff]/[ensurePeriodicSweepScheduled] stay no-ops: they need `BGTaskScheduler`, a
 * separate, still-unbuilt piece of iOS infra. This one doesn't - it's a single local,
 * future-dated notification, and iOS delivers it itself once armed via
 * `UNUserNotificationCenter`, no background execution needed to schedule it.
 *
 * Mirrors `AndroidWorkManagerScheduler.scheduleRecapReadyNotification`'s contract: the same
 * cross-platform [durationUntilRecapNotification] for the delay math, and
 * `"recap-ready-$tripId"` as the request identifier so a repeat call overwrites in place
 * (`addNotificationRequest` with a reused identifier replaces the pending request) rather than
 * stacking duplicates. No `UNUserNotificationCenterDelegate` is registered, so a tap just
 * launches the app normally (Home), the same minimal UX as Android's plain `MainActivity`
 * launch intent - no deep link into the Recap stack.
 */
public class IosBackgroundWorkScheduler(
    private val clock: Clock = Clock.System,
) : BackgroundWorkScheduler {
    override fun scheduleOneOff(kind: BackgroundJobKind) {
        // No-op - needs BGTaskScheduler, out of scope for this milestone (docs/roadmap.md M21.3).
    }

    override fun ensurePeriodicSweepScheduled() {
        // No-op - needs BGTaskScheduler, out of scope for this milestone (docs/roadmap.md M21.3).
    }

    override fun scheduleRecapReadyNotification(
        tripId: String,
        fireAt: LocalDate,
    ) {
        val delaySeconds =
            durationUntilRecapNotification(fireAt, clock.now())
                .inWholeSeconds
                .toDouble()
                .coerceAtLeast(MIN_TRIGGER_INTERVAL_SECONDS)
        val content =
            UNMutableNotificationContent().apply {
                setTitle("Your recap is ready")
                setBody("You and your partner's trip recap is ready to view.")
            }
        val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(delaySeconds, repeats = false)
        val request =
            UNNotificationRequest.requestWithIdentifier(
                identifier = "recap-ready-$tripId",
                content = content,
                trigger = trigger,
            )
        UNUserNotificationCenter
            .currentNotificationCenter()
            .addNotificationRequest(request, withCompletionHandler = null)
    }
}
