package com.alongside.feature.places

import com.alongside.core.domain.work.BackgroundJobKind
import com.alongside.core.domain.work.BackgroundWorkScheduler

internal class FakeBackgroundWorkScheduler : BackgroundWorkScheduler {
    val scheduledOneOffs = mutableListOf<BackgroundJobKind>()
    var periodicSweepEnsured: Boolean = false
        private set

    override fun scheduleOneOff(kind: BackgroundJobKind) {
        scheduledOneOffs += kind
    }

    override fun ensurePeriodicSweepScheduled() {
        periodicSweepEnsured = true
    }
}
