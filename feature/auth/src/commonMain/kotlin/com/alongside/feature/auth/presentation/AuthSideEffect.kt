package com.alongside.feature.auth.presentation

public sealed interface AuthSideEffect {
    /** Hook for navigation once M6+ introduces a nav graph; unused by M5 itself. */
    public data object SignedIn : AuthSideEffect
}
