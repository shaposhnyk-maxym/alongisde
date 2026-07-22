package com.alongside.core.domain.place

import com.alongside.core.model.place.MatchStatus
import com.alongside.core.model.place.SwipeDirection

public fun resolveMatchStatus(
    ownerSwipe: SwipeDirection?,
    memberSwipe: SwipeDirection?,
): MatchStatus {
    if (ownerSwipe == null || memberSwipe == null) {
        return MatchStatus.PENDING
    }
    return when {
        ownerSwipe == SwipeDirection.LIKE && memberSwipe == SwipeDirection.LIKE -> MatchStatus.MATCHED
        ownerSwipe == SwipeDirection.DISLIKE && memberSwipe == SwipeDirection.DISLIKE -> MatchStatus.REJECTED
        else -> MatchStatus.PENDING
    }
}

/**
 * Whether a PENDING candidate still needs *my* decision - true if I haven't swiped yet, or if
 * we've both swiped and disagree (a split, offered back for reconsideration). False only when
 * I've already decided and I'm just waiting on the other side.
 */
public fun isMyTurn(
    mine: SwipeDirection?,
    theirs: SwipeDirection?,
): Boolean = mine == null || (theirs != null && theirs != mine)
