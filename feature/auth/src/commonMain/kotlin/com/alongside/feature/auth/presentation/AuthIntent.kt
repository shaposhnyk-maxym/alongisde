package com.alongside.feature.auth.presentation

public sealed interface AuthIntent {
    public data object SignInWithGoogle : AuthIntent

    public data object DismissError : AuthIntent
}
