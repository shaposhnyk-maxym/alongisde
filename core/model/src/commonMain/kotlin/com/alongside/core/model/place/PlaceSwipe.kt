package com.alongside.core.model.place

import com.alongside.core.model.SyncStatus
import kotlin.time.Instant

/**
 * One user's decision on one [PlaceCandidate] - never a field on a document another user also
 * writes. [id] is deterministic (`"$candidateId::$userId"`), so a user re-swiping the same
 * candidate always overwrites their own prior record, never anyone else's - the write conflict
 * that motivated this entity (two trip members racing to overwrite [PlaceCandidate]'s old
 * shared `ownerSwipe`/`memberSwipe` fields) is structurally impossible here, since a record's
 * owner is baked into its id.
 */
public data class PlaceSwipe(
    val id: String,
    val tripId: String,
    val candidateId: String,
    val userId: String,
    val direction: SwipeDirection,
    val swipedAt: Instant,
    val syncStatus: SyncStatus,
    val updatedAt: Instant,
)
