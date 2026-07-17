package com.alongside.feature.auth.presentation

import androidx.lifecycle.ViewModel
import com.alongside.core.domain.auth.AuthException
import com.alongside.core.domain.auth.AuthSessionCache
import com.alongside.core.domain.auth.AuthSessionRepository
import com.alongside.core.domain.auth.isExpired
import com.alongside.feature.auth.GoogleAuthProvider
import com.alongside.feature.auth.GoogleSignInResult
import kotlinx.coroutines.suspendCancellableCoroutine
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.container
import kotlin.coroutines.resume

public class AuthContainer(
    private val googleAuthProvider: GoogleAuthProvider,
    private val authSessionRepository: AuthSessionRepository,
    private val authSessionCache: AuthSessionCache,
) : ViewModel(),
    ContainerHost<AuthState, AuthSideEffect> {
    override val container: Container<AuthState, AuthSideEffect> =
        container(AuthState()) { restoreSession() }

    public fun onIntent(intent: AuthIntent) {
        when (intent) {
            AuthIntent.SignInWithGoogle -> signInWithGoogle()
            AuthIntent.DismissError -> dismissError()
        }
    }

    // Runs once when the container is created. A still-valid cached session is restored with no
    // network call at all; an expired one is silently re-authenticated (no UI - see
    // GoogleAuthProvider.signInSilently) rather than forcing the user through the sign-in button
    // again. Either kind of failure here is invisible on purpose: this is a background restore
    // attempt the user never triggered, so it falls back to the normal idle/sign-in-button state
    // rather than surfacing an error banner.
    private suspend fun Syntax<AuthState, AuthSideEffect>.restoreSession() {
        val cached = authSessionCache.get() ?: return
        if (cached.isExpired()) {
            refreshExpiredSession()
        } else {
            reduce { state.copy(session = cached) }
        }
    }

    private suspend fun Syntax<AuthState, AuthSideEffect>.refreshExpiredSession() {
        val result = awaitGoogleSignInSilently()
        val googleIdToken = (result as? GoogleSignInResult.Success)?.idToken
        if (googleIdToken == null) {
            println("AuthContainer: silent session restore failed at the provider - $result")
            authSessionCache.clear()
            return
        }
        try {
            val session = authSessionRepository.signInWithGoogle(googleIdToken)
            authSessionCache.save(session)
            reduce { state.copy(session = session) }
            postSideEffect(AuthSideEffect.SignedIn)
        } catch (e: AuthException) {
            println("AuthContainer: silent session restore failed during token exchange: ${e.message}")
            authSessionCache.clear()
        }
    }

    private fun signInWithGoogle() =
        intent {
            reduce { state.copy(isSigningIn = true, error = null) }
            when (val result = awaitGoogleSignIn()) {
                is GoogleSignInResult.Success -> exchangeToken(result.idToken)
                GoogleSignInResult.Cancelled -> {
                    println("AuthContainer: sign-in cancelled by user")
                    reduce { state.copy(isSigningIn = false) }
                }
                is GoogleSignInResult.Failure -> {
                    println("AuthContainer: provider failure - ${result.message}")
                    reduce { state.copy(isSigningIn = false, error = AuthError.SIGN_IN_FAILED) }
                }
            }
        }

    private fun dismissError() =
        intent {
            reduce { state.copy(error = null) }
        }

    // Logged via println (no multiplatform logging infra yet) rather than swallowed - the
    // exception's type still drives the AuthError mapped into state below, but the underlying
    // message/cause is otherwise invisible (no crash, nothing in the UI beyond a generic banner).
    private suspend fun Syntax<AuthState, AuthSideEffect>.exchangeToken(idToken: String) {
        try {
            val session = authSessionRepository.signInWithGoogle(idToken)
            authSessionCache.save(session)
            println("AuthContainer: signed in as ${session.user.email ?: session.user.uid}")
            reduce { state.copy(isSigningIn = false, session = session) }
            postSideEffect(AuthSideEffect.SignedIn)
        } catch (e: AuthException.InvalidToken) {
            println("AuthContainer: token exchange failed - invalid token: ${e.message}")
            reduce { state.copy(isSigningIn = false, error = AuthError.INVALID_TOKEN) }
        } catch (e: AuthException.Network) {
            println("AuthContainer: token exchange failed - network: ${e.message}, cause=${e.cause}")
            reduce { state.copy(isSigningIn = false, error = AuthError.NETWORK) }
        } catch (e: AuthException.Unknown) {
            println("AuthContainer: token exchange failed - unknown: ${e.message}, cause=${e.cause}")
            reduce { state.copy(isSigningIn = false, error = AuthError.SIGN_IN_FAILED) }
        }
    }

    private suspend fun awaitGoogleSignIn(): GoogleSignInResult =
        suspendCancellableCoroutine { continuation ->
            googleAuthProvider.signIn { result -> continuation.resume(result) }
        }

    private suspend fun awaitGoogleSignInSilently(): GoogleSignInResult =
        suspendCancellableCoroutine { continuation ->
            googleAuthProvider.signInSilently { result -> continuation.resume(result) }
        }
}
