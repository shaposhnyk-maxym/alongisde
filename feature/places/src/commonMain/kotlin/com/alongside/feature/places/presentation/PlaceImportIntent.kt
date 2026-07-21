package com.alongside.feature.places.presentation

public sealed interface PlaceImportIntent {
    public data object Accept : PlaceImportIntent

    public data object Discard : PlaceImportIntent
}
