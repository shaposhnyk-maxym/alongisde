package com.alongside.feature.pairing.presentation

public sealed interface PairingSideEffect {
    /** Both people are on the trip — pairing is complete, navigation can leave this screen. */
    public data object Paired : PairingSideEffect
}
