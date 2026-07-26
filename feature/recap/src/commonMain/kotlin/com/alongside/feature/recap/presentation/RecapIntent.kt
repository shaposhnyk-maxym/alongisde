package com.alongside.feature.recap.presentation

public sealed interface RecapIntent {
    public data class ChangeActiveSlide(
        val index: Int,
    ) : RecapIntent
}
