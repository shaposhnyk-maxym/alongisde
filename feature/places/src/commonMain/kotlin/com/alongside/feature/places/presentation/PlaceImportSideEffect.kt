package com.alongside.feature.places.presentation

public sealed interface PlaceImportSideEffect {
    /** Accepted - the place was persisted, navigation can leave this screen. */
    public data object Imported : PlaceImportSideEffect

    /** Discarded (or nothing to keep - not-found/error dismissed), nothing was persisted. */
    public data object Discarded : PlaceImportSideEffect
}
