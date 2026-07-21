package com.alongside.app.work

import com.alongside.core.domain.work.BackgroundJobKind
import com.alongside.core.domain.work.BackgroundWorkScheduler

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
}
