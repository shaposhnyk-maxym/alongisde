package com.alongside.feature.recap.di

import com.alongside.feature.recap.presentation.RecapContainer
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

public val recapFeatureModule =
    module {
        viewModel { RecapContainer(get(), get(), get(), get(), get(), get(), get()) }
    }
