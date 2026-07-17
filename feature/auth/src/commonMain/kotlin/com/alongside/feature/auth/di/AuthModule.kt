package com.alongside.feature.auth.di

import com.alongside.feature.auth.GoogleAuthProvider
import com.alongside.feature.auth.presentation.AuthContainer
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** AuthContainer needs a [GoogleAuthProvider] at creation time - callers pass it via `parametersOf`. */
public val authFeatureModule =
    module {
        viewModel { (provider: GoogleAuthProvider) -> AuthContainer(provider, get(), get()) }
    }
