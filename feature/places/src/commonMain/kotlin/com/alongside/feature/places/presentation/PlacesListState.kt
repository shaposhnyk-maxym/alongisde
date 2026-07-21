package com.alongside.feature.places.presentation

import androidx.compose.runtime.Immutable
import com.alongside.core.model.place.PlaceCandidate

@Immutable
public data class PlacesListState(
    val places: List<PlaceCandidate> = emptyList(),
    val isLoading: Boolean = true,
)
