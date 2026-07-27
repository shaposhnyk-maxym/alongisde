package com.alongside.app.work

import com.alongside.core.domain.work.BackgroundJobKind
import com.alongside.core.domain.work.BackgroundWorkScheduler
import kotlinx.datetime.LocalDate

/**
 * Placeholder [BackgroundWorkScheduler] for iOS - no `iosApp` Xcode project exists in this repo
 * yet (same Apple dev account blocker as docs/roadmap.md M7), so there is nowhere to register a
 * real `BGTaskScheduler` task identifier (`Info.plist`, `AppDelegate`) against. Building a
 * `BGTaskScheduler`-backed implementation now would compile but could never actually run. Both
 * methods are intentionally no-ops until that shell exists - see docs/roadmap.md M12.11.
 */
public class NoOpBackgroundWorkScheduler : BackgroundWorkScheduler {
    override fun scheduleOneOff(kind: BackgroundJobKind) {
        // No-op - see class kdoc.
    }

    override fun ensurePeriodicSweepScheduled() {
        // No-op - see class kdoc.
    }

    // ============================================================================================
    // TODO(M20.5-ios) — BIG TODO, DO NOT LOSE TRACK OF THIS. Real work, not a stub-forever no-op.
    // ============================================================================================
    // Android already ships a real local "your recap is ready" notification (docs/roadmap.md
    // M20.5): WorkManager fires it at `recapAvailableAt(tripEndDate)`, entirely locally, no
    // Firebase/server involved, because both partners' devices compute that date identically and
    // independently. iOS needs the equivalent, and right now gets nothing - a real user on iOS
    // will never be told their recap is ready.
    //
    // Blocked on the same thing every other iOS-native feature here is (Apple dev account, see
    // docs/roadmap.md M7/M12.11) - there's no `iosApp` Xcode project to register a
    // `UNUserNotificationCenter` request against yet. Once that shell exists:
    //  1. Request `UNAuthorizationOptions.alert` (+ `.sound` if wanted) once, at an
    //     onboarding-equivalent moment - NOT inside this method, this class stays a pure
    //     schedule-only seam.
    //  2. Build a `UNMutableNotificationContent` (title/body - reuse the same copy Android's
    //     `RecapReadyNotificationWorker` uses, "Your recap is ready", for consistency) and
    //     schedule it via `UNCalendarNotificationTrigger` keyed off `fireAt`'s local midnight, or
    //     `UNTimeIntervalNotificationTrigger` fed by this repo's own cross-platform
    //     `durationUntilRecapNotification(fireAt, Clock.System.now())`
    //     (`core/domain/.../recap/RecapNotificationTiming.kt`) - REUSE that function, don't
    //     re-derive the date math a second time.
    //  3. Use notification identifier `"recap-ready-$tripId"` on the `UNNotificationRequest` so a
    //     repeat call to this method overwrites in place (`addNotificationRequest` with an
    //     existing identifier replaces it) - the same idempotent-repeat-call contract
    //     `ExistingWorkPolicy.KEEP` gives the Android implementation for free.
    //  4. Wire a `UNUserNotificationCenterDelegate` so tapping the notification opens the app on
    //     Home (not a deep link straight into the Recap stack) - same minimalism as Android's
    //     plain `MainActivity` launch intent.
    override fun scheduleRecapReadyNotification(
        tripId: String,
        fireAt: LocalDate,
    ) {
        // No-op - see the TODO block above. Silently does nothing until the iosApp shell exists.
    }
}
