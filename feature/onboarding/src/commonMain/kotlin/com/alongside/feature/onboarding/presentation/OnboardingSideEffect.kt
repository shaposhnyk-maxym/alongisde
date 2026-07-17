package com.alongside.feature.onboarding.presentation

public sealed interface OnboardingSideEffect {
    /** Hook for navigation once M6+ introduces a nav graph; unused by M6 itself. */
    public data object Completed : OnboardingSideEffect
}
