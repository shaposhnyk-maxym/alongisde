package com.alongside.feature.matcher.presentation

import androidx.compose.runtime.Immutable
import com.alongside.core.domain.place.isMyTurn
import com.alongside.core.domain.place.resolveMatchStatus
import com.alongside.core.model.place.MatchStatus
import com.alongside.core.model.place.PlaceCandidate
import com.alongside.core.model.place.PlaceSwipe
import com.alongside.core.model.trip.Trip

/**
 * [deck]/[matches] are derived, never stored, the same "recompute from raw data" convention as
 * `PairingState.step` - a candidate's membership in either list can never drift out of sync with
 * [candidates]/[swipes] because there's no separate flag tracking it.
 *
 * A candidate whose sides disagree (one LIKE, one DISLIKE) resolves to [MatchStatus.PENDING] -
 * the exact same bucket as "nobody has swiped yet" - so it stays in [deck] and gets shown again,
 * without either [PlaceSwipe] record ever needing to be reset or touched by the other user.
 */
@Immutable
public data class MatcherState(
    val ownUserId: String? = null,
    val trip: Trip? = null,
    val candidates: List<PlaceCandidate> = emptyList(),
    val swipes: List<PlaceSwipe> = emptyList(),
) {
    val deck: List<PlaceCandidate>
        get() = candidates.filter { matchStatus(it) == MatchStatus.PENDING }

    val matches: List<PlaceCandidate>
        get() = candidates.filter { matchStatus(it) == MatchStatus.MATCHED }

    /**
     * Which [deck] candidates still need *my* decision: fresh ones, ones my partner already
     * decided on, and splits offered back for reconsideration - excludes only "I've decided,
     * partner hasn't yet" (nothing to do but wait). Doesn't determine display order - that's a
     * UI-layer concern (see `MatcherContent`'s local queue).
     */
    val myTurnDeck: List<PlaceCandidate>
        get() {
            val uid = ownUserId ?: return emptyList()
            return deck.filter { candidate ->
                isMyTurn(
                    mine = swipeDirection(candidate.id, uid),
                    theirs = otherSwipeDirection(candidate.id, uid),
                )
            }
        }

    internal fun matchStatus(candidate: PlaceCandidate): MatchStatus {
        val trip = trip ?: return MatchStatus.PENDING
        val ownerSwipe = swipeDirection(candidate.id, trip.ownerId)
        val memberSwipe = trip.memberId?.let { swipeDirection(candidate.id, it) }
        return resolveMatchStatus(ownerSwipe, memberSwipe)
    }

    private fun swipeDirection(
        candidateId: String,
        userId: String,
    ) = swipes.find { it.candidateId == candidateId && it.userId == userId }?.direction

    private fun otherSwipeDirection(
        candidateId: String,
        uid: String,
    ) = swipes.find { it.candidateId == candidateId && it.userId != uid }?.direction
}
