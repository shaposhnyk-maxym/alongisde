package com.alongside.feature.recap.presentation

import androidx.compose.runtime.Immutable
import com.alongside.core.model.recap.RecapSlide

@Immutable
public data class RecapState(
    val isLoading: Boolean = true,
    val slides: List<RecapSlide> = emptyList(),
    val activeIndex: Int = 0,
)
