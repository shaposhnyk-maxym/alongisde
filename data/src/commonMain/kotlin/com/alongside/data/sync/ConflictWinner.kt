package com.alongside.data.sync

import kotlin.time.Instant

public enum class ConflictWinner { LOCAL, REMOTE }

/**
 * Last-write-wins by client-set `updatedAt`. LOCAL wins ties (`>=`) and the no-remote case:
 * when both sides claim the same instant there is no basis to prefer the remote copy, and
 * pushing keeps the outcome deterministic from the local device's point of view.
 *
 * Compared at millisecond precision deliberately, not full [Instant] precision: Room stores
 * `updatedAt` as epoch millis (`AlongsideTypeConverters.fromInstant` truncates), while Firestore
 * round-trips the value's full sub-millisecond remainder from `Instant#toString()`. Comparing the
 * raw instants means an entity that hasn't changed at all still looks "remote-newer" by whatever
 * sub-millisecond dust survived the round trip - forcing a needless re-save on literally every
 * poll tick forever, which re-emits Room's observing Flow every time, which re-fires anything
 * downstream (e.g. a Paired side effect) right along with it. Room can never represent more than
 * millisecond precision anyway, so that's the only granularity a "real" change can show up at.
 */
public fun resolveConflict(
    localUpdatedAt: Instant,
    remoteUpdatedAt: Instant?,
): ConflictWinner =
    if (remoteUpdatedAt == null || localUpdatedAt.toEpochMilliseconds() >= remoteUpdatedAt.toEpochMilliseconds()) {
        ConflictWinner.LOCAL
    } else {
        ConflictWinner.REMOTE
    }
