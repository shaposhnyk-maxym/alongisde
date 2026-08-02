package com.alongside.feature.matcher.presentation

import androidx.compose.runtime.Immutable
import com.alongside.core.domain.place.isMyTurn
import com.alongside.core.domain.place.resolveMatchStatus
import com.alongside.core.model.place.MatchStatus
import com.alongside.core.model.place.PlaceCandidate
import com.alongside.core.model.place.PlaceSwipe
import com.alongside.core.model.place.SwipeDirection
import com.alongside.core.model.trip.Trip

/**
 * [deck]/[matches] are derived, never stored, the same "recompute from raw data" convention as
 * `PairingState.step` - a candidate's membership in either list can never drift out of sync with
 * [candidates]/[swipes] because there's no separate flag tracking it.
 *
 * A candidate whose sides disagree (one LIKE, one DISLIKE) resolves to [MatchStatus.PENDING] -
 * the exact same bucket as "nobody has swiped yet" - so it stays in [deck] and gets shown again,
 * without either [PlaceSwipe] record ever needing to be reset or touched by the other user.
 *
 * [deck] additionally excludes candidates the current user imported themselves
 * (docs/roadmap.md M21.5) - swiping on a place you just picked yourself defeats the "element of
 * surprise" the deck exists for. [matches] is NOT filtered this way: a match already required
 * both sides to say yes, so who originally imported it is no longer relevant once it's mutual.
 *
 * Since the importer never sees their own import in [deck] to cast a real vote, [matchStatus]
 * gives them an implicit LIKE via [effectiveSwipe] - without it, a self-imported candidate could
 * never match no matter how the partner swiped. That implicit vote is intentionally NOT a real
 * [PlaceSwipe] and stays out of [swipeDirection]/[otherSwipeDirection] (which back [myTurnDeck]
 * via [isMyTurn]) - a real, permanent LIKE there would make every partner DISLIKE an eternal
 * "still my turn" split (the importer's side never changes), so a single-card deck could never be
 * swiped away, only matched. Keeping the implied vote resolution-only lets a partner's dislike
 * cleanly drop the candidate out of their [myTurnDeck] instead.
 */
@Immutable
public data class MatcherState(
    val ownUserId: String? = null,
    val trip: Trip? = null,
    val candidates: List<PlaceCandidate> = emptyList(),
    val swipes: List<PlaceSwipe> = emptyList(),
) {
    val deck: List<PlaceCandidate>
        get() =
            candidates.filter { matchStatus(it) == MatchStatus.PENDING && it.addedByUserId != ownUserId }

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
        val ownerSwipe = effectiveSwipe(candidate, trip.ownerId)
        val memberSwipe = trip.memberId?.let { effectiveSwipe(candidate, it) }
        return resolveMatchStatus(ownerSwipe, memberSwipe)
    }

    private fun effectiveSwipe(
        candidate: PlaceCandidate,
        userId: String,
    ) = swipeDirection(candidate.id, userId)
        ?: SwipeDirection.LIKE.takeIf { candidate.addedByUserId == userId }

    private fun swipeDirection(
        candidateId: String,
        userId: String,
    ) = swipes.find { it.candidateId == candidateId && it.userId == userId }?.direction

    private fun otherSwipeDirection(
        candidateId: String,
        uid: String,
    ) = swipes.find { it.candidateId == candidateId && it.userId != uid }?.direction
}
