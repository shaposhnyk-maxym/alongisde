package com.alongside.feature.auth.presentation

import androidx.compose.runtime.Immutable
import com.alongside.core.model.auth.AuthSession

@Immutable
data class AuthState(
    val isRestoringSession: Boolean = true,
    val isSigningIn: Boolean = false,
    val session: AuthSession? = null,
    val error: AuthError? = null,
)
