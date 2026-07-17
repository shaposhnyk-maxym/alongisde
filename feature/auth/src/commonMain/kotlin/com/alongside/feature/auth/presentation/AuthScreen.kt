package com.alongside.feature.auth.presentation

import alongside.feature.auth.generated.resources.Res
import alongside.feature.auth.generated.resources.alongside
import alongside.feature.auth.generated.resources.alongside_slogan
import alongside.feature.auth.generated.resources.continue_with_google
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alongside.core.model.auth.AuthSession
import com.alongside.core.model.auth.AuthUser
import com.alongside.core.ui.animation.PulsingDot
import com.alongside.core.ui.component.AlongsideOnPaperButton
import com.alongside.core.ui.component.DotBanner
import com.alongside.core.ui.component.InkGradientBackground
import com.alongside.core.ui.theme.AlongsideSpacing
import com.alongside.core.ui.theme.AlongsideTheme
import com.alongside.core.ui.theme.alongsideTypography
import org.jetbrains.compose.resources.stringResource
import org.orbitmvi.orbit.compose.collectAsState
import kotlin.time.Clock

@Composable
public fun AuthScreen(
    container: AuthContainer,
    modifier: Modifier = Modifier,
) {
    val state by container.collectAsState()
    AuthContent(
        state = state,
        onSignInClick = { container.onIntent(AuthIntent.SignInWithGoogle) },
        modifier = modifier,
    )
}

@Composable
internal fun AuthContent(
    state: AuthState,
    onSignInClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    InkGradientBackground(modifier = modifier.fillMaxSize()) {
        Box(
            Modifier.fillMaxSize().navigationBarsPadding().padding(
                start = 16.dp,
                end = 16.dp,
                bottom = 64.dp,
            ),
        ) {
            Column(
                modifier =
                    Modifier.align(Alignment.BottomCenter),
                verticalArrangement = Arrangement.spacedBy(AlongsideSpacing.xxl),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.alongside),
                        style =
                            MaterialTheme.alongsideTypography.displaySerifItalic.copy(
                                textAlign = TextAlign.Start,
                            ),
                    )

                    Text(
                        text = stringResource(Res.string.alongside_slogan),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                val session = state.session
                if (session != null) {
                    Text(
                        text = "Signed in as ${session.user.email ?: session.user.displayName ?: session.user.uid}",
                        style = MaterialTheme.alongsideTypography.meta,
                    )
                } else {
                    AlongsideOnPaperButton(
                        text = stringResource(Res.string.continue_with_google),
                        onClick = onSignInClick,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isSigningIn,
                    )
                    if (state.isSigningIn) {
                        PulsingDot()
                    }
                }
                state.error?.let { error ->
                    DotBanner(text = error.toMessage())
                }
            }
        }
    }
}

private fun AuthError.toMessage(): String =
    when (this) {
        AuthError.NETWORK -> "Couldn't reach the network. Check your connection and try again."
        AuthError.INVALID_TOKEN -> "That sign-in didn't go through. Please try again."
        AuthError.SIGN_IN_FAILED -> "Sign-in failed. Please try again."
    }

private val PreviewSize = Modifier.size(360.dp, 640.dp)

@Preview
@Composable
private fun AuthScreenIdlePreview() {
    AlongsideTheme {
        AuthContent(state = AuthState(), onSignInClick = {}, modifier = PreviewSize)
    }
}

@Preview
@Composable
private fun AuthScreenLoadingPreview() {
    AlongsideTheme {
        AuthContent(state = AuthState(isSigningIn = true), onSignInClick = {}, modifier = PreviewSize)
    }
}

@Preview
@Composable
private fun AuthScreenErrorPreview() {
    AlongsideTheme {
        AuthContent(state = AuthState(error = AuthError.NETWORK), onSignInClick = {}, modifier = PreviewSize)
    }
}

@Preview
@Composable
private fun AuthScreenSignedInPreview() {
    AlongsideTheme {
        val session =
            AuthSession(
                user =
                    AuthUser(
                        uid = "uid-1",
                        email = "person@example.com",
                        displayName = "Person One",
                        photoUrl = null,
                    ),
                idToken = "id-token",
                refreshToken = null,
                expiresInSeconds = 3600L,
                issuedAt = Clock.System.now(),
            )
        AuthContent(state = AuthState(session = session), onSignInClick = {}, modifier = PreviewSize)
    }
}
