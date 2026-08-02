package com.alongside.feature.places.di

import com.alongside.feature.places.presentation.PlaceContentPullCoordinator
import com.alongside.feature.places.presentation.PlaceImportContainer
import com.alongside.feature.places.presentation.PlaceRetryCoordinator
import com.alongside.feature.places.presentation.PlacesListContainer
import com.alongside.feature.places.presentation.PlacesListDataSource
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** PlaceImportContainer needs the raw shared text at creation time - callers pass it via `parametersOf`. */
public val placesFeatureModule =
    module {
        viewModel { (shareText: String) -> PlaceImportContainer(shareText, get(), get(), get(), get(), get()) }
        single { PlaceRetryCoordinator(get(), get(), get()) }
        single { PlaceContentPullCoordinator(get(), get()) }
        single { PlacesListDataSource(get(), get(), get()) }
        viewModel { PlacesListContainer(get(), get()) }
    }
