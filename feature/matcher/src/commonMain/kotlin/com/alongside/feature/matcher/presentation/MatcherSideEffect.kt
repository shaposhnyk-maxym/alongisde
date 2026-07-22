package com.alongside.feature.matcher.presentation

import com.alongside.core.model.place.PlaceCandidate

public sealed interface MatcherSideEffect {
    /**
     * Fires the moment a candidate's derived status newly becomes [com.alongside.core.model.place.MatchStatus.MATCHED]
     * - whether that transition was caused by this device's own swipe or a partner's swipe
     * arriving via sync - so M15's UI can react (e.g. a celebration animation) exactly once per
     * match, not on every subsequent state emission that still includes it.
     */
    public data class Matched(
        val place: PlaceCandidate,
    ) : MatcherSideEffect
}
