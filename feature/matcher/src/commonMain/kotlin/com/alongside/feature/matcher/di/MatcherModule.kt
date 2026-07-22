package com.alongside.feature.matcher.di

import com.alongside.feature.matcher.presentation.MatcherContainer
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

public val matcherFeatureModule =
    module {
        viewModel { MatcherContainer(get(), get(), get(), get(), get()) }
    }
