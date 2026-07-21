package com.alongside.feature.places.presentation

import androidx.compose.runtime.Immutable
import com.alongside.core.model.place.PlaceCandidate

public enum class PlaceImportStatus {
    LOADING,
    FOUND,
    NOT_FOUND,
    ERROR,
}

@Immutable
public data class PlaceImportState(
    val status: PlaceImportStatus = PlaceImportStatus.LOADING,
    val place: PlaceCandidate? = null,
    val errorMessage: String? = null,
)
