package com.alongside.app.di

import com.alongside.app.home.HomeContainer
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

public val appModule =
    module {
        viewModel { HomeContainer(get(), get(), get(), get(), get(), get(), get()) }
    }
