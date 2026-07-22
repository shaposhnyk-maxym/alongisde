package com.alongside.feature.matcher.presentation

import com.alongside.core.model.place.SwipeDirection

public sealed interface MatcherIntent {
    public data class Swipe(
        val candidateId: String,
        val direction: SwipeDirection,
    ) : MatcherIntent
}
