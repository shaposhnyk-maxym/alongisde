package com.alongside.feature.settings.di

import com.alongside.feature.settings.presentation.SettingsContainer
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

public val settingsFeatureModule =
    module {
        viewModel { SettingsContainer(get(), get(), get()) }
    }
