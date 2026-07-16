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
