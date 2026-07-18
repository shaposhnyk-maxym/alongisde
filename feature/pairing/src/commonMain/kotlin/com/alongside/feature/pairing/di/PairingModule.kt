package com.alongside.feature.pairing.di

import com.alongside.feature.pairing.presentation.PairingContainer
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

public val pairingFeatureModule =
    module {
        viewModel { PairingContainer(get(), get()) }
    }
