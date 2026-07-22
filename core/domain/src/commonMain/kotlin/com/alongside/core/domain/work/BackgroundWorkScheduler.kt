package com.alongside.core.domain.work

/** Reliability job kinds that can be handed off to a durable background scheduler. */
public enum class BackgroundJobKind { EPISODE_RETRY, PLACE_RETRY, SYNC_QUEUE_FLUSH, PLACE_CONTENT_PULL }

/**
 * Seam over whatever platform mechanism survives app closure/process death/device reboot
 * (WorkManager on Android, `BGTaskScheduler` on iOS) - a plain interface, not `expect`/`actual`,
 * the same shape as [com.alongside.core.domain.diary.EpisodeRepository]: platform implementations
 * are injected via Koin at the composition root, not resolved through compiler-enforced platform
 * seams (those are for top-level functions with no natural class-based polymorphism, e.g. this
 * project's `PhotoPickerLauncher`).
 */
public interface BackgroundWorkScheduler {
    /** Event-driven: enqueue right after an action leaves [kind]'s work incomplete. */
    public fun scheduleOneOff(kind: BackgroundJobKind)

    /**
     * Called once at app startup; idempotent. Schedules a single periodic sweep covering every
     * [BackgroundJobKind] in one pass - they all share the same expensive prefix (resolve the
     * current user, then their active trip), so one periodic job is strictly better than one per
     * kind. This is a backstop for a missed [scheduleOneOff], not the primary reliability path.
     */
    public fun ensurePeriodicSweepScheduled()
}
