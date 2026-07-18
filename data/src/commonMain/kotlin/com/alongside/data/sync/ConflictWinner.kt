package com.alongside.data.sync

import kotlin.time.Instant

public enum class ConflictWinner { LOCAL, REMOTE }

/**
 * Last-write-wins by client-set `updatedAt`. LOCAL wins ties (`>=`) and the no-remote case:
 * when both sides claim the same instant there is no basis to prefer the remote copy, and
 * pushing keeps the outcome deterministic from the local device's point of view.
 */
public fun resolveConflict(
    localUpdatedAt: Instant,
    remoteUpdatedAt: Instant?,
): ConflictWinner =
    if (remoteUpdatedAt == null || localUpdatedAt >= remoteUpdatedAt) {
        ConflictWinner.LOCAL
    } else {
        ConflictWinner.REMOTE
    }
