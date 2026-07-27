package com.alongside.data

import com.alongside.core.domain.work.BackgroundJobKind
import com.alongside.core.domain.work.BackgroundWorkScheduler
import kotlinx.datetime.LocalDate

internal class FakeBackgroundWorkScheduler : BackgroundWorkScheduler {
    val scheduledOneOffs = mutableListOf<BackgroundJobKind>()
    var periodicSweepEnsured: Boolean = false
        private set
    val scheduledRecapNotifications = mutableListOf<Pair<String, LocalDate>>()

    override fun scheduleOneOff(kind: BackgroundJobKind) {
        scheduledOneOffs += kind
    }

    override fun ensurePeriodicSweepScheduled() {
        periodicSweepEnsured = true
    }

    override fun scheduleRecapReadyNotification(
        tripId: String,
        fireAt: LocalDate,
    ) {
        scheduledRecapNotifications += tripId to fireAt
    }
}
