package com.alongside.feature.onboarding.di

import com.alongside.feature.onboarding.PermissionController
import com.alongside.feature.onboarding.presentation.OnboardingContainer
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** OnboardingContainer needs a [PermissionController] at creation time - callers pass it via `parametersOf`. */
public val onboardingFeatureModule =
    module {
        viewModel { (controller: PermissionController) -> OnboardingContainer(controller) }
    }
