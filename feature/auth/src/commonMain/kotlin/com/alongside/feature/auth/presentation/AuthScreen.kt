package com.alongside.feature.auth.presentation

import alongside.feature.auth.generated.resources.Res
import alongside.feature.auth.generated.resources.alongside
import alongside.feature.auth.generated.resources.alongside_slogan
import alongside.feature.auth.generated.resources.auth_terms_caption
import alongside.feature.auth.generated.resources.continue_with_google
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alongside.core.model.auth.AuthSession
import com.alongside.core.model.auth.AuthUser
import com.alongside.core.ui.animation.FadeUpReveal
import com.alongside.core.ui.animation.PulsingDot
import com.alongside.core.ui.component.AlongsideOnPaperButton
import com.alongside.core.ui.component.DotBanner
import com.alongside.core.ui.component.InkGradientBackground
import com.alongside.core.ui.component.PaperCard
import com.alongside.core.ui.theme.AlongsideSpacing
import com.alongside.core.ui.theme.AlongsideTheme
import com.alongside.core.ui.theme.alongsideColors
import com.alongside.core.ui.theme.alongsideTypography
import org.jetbrains.compose.resources.stringResource
import org.orbitmvi.orbit.compose.collectAsState
import kotlin.time.Clock

private const val CARD_REVEAL_DELAY_MILLIS = 150L

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
    animateEntrance: Boolean = true,
) {
    InkGradientBackground(modifier = modifier.fillMaxSize()) {
        HeroBackdrop(modifier = Modifier.fillMaxSize())

        // While a cached session is still being checked/silently refreshed, showing the sign-in
        // button would flash it in front of an already-authenticated user for one frame before
        // the nav graph advances past Login - simplest fix is to just not draw it yet.
        if (!state.isRestoringSession) {
            Column(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(start = 20.dp, end = 20.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(AlongsideSpacing.xxl),
            ) {
                FadeUpReveal(initiallyRevealed = !animateEntrance) {
                    BrandBlock(modifier = Modifier.padding(horizontal = AlongsideSpacing.sm))
                }

                state.error?.let { error ->
                    DotBanner(text = error.toMessage(), modifier = Modifier.fillMaxWidth())
                }

                FadeUpReveal(
                    delayMillis = CARD_REVEAL_DELAY_MILLIS,
                    initiallyRevealed = !animateEntrance,
                ) {
                    SignInCard(state = state, onSignInClick = onSignInClick)
                }
            }
        }
    }
}

@Composable
private fun BrandBlock(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AlongsideSpacing.sm),
    ) {
        Text(
            text = stringResource(Res.string.alongside),
            style = MaterialTheme.alongsideTypography.displaySerifItalic,
        )
        Text(
            text = stringResource(Res.string.alongside_slogan),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SignInCard(
    state: AuthState,
    onSignInClick: () -> Unit,
) {
    PaperCard(modifier = Modifier.fillMaxWidth()) {
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
                modifier = Modifier.fillMaxWidth().glowPulse(),
                enabled = !state.isSigningIn,
            )
            if (state.isSigningIn) {
                PulsingDot(
                    modifier =
                        Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = AlongsideSpacing.md),
                )
            }
            Text(
                text = stringResource(Res.string.auth_terms_caption),
                modifier = Modifier.fillMaxWidth().padding(top = AlongsideSpacing.md),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.alongsideColors.onPaperSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private const val GLOW_PERIOD_MILLIS = 2400
private const val GLOW_START_ALPHA = 0.35f
private val GlowMaxSpread = 8.dp
private val GlowCornerRadius = 14.dp

/** The design's `glow` keyframes: a terracotta ring swelling out from the button and fading. */
@Composable
private fun Modifier.glowPulse(): Modifier {
    val color = MaterialTheme.colorScheme.primary
    val transition = rememberInfiniteTransition()
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(GLOW_PERIOD_MILLIS, easing = LinearEasing)),
    )
    return drawBehind {
        val spread = GlowMaxSpread.toPx() * progress
        val alpha = GLOW_START_ALPHA * (1f - progress)
        if (alpha > 0f) {
            drawRoundRect(
                color = color,
                topLeft = Offset(-spread, -spread),
                size = Size(size.width + spread * 2, size.height + spread * 2),
                cornerRadius = CornerRadius(GlowCornerRadius.toPx() + spread),
                alpha = alpha,
            )
        }
    }
}

private const val HERO_GLOW_CENTER_X = 0.72f
private const val HERO_GLOW_CENTER_Y = 0.30f
private const val HERO_GLOW_RADIUS = 0.95f
private const val HERO_GLOW_ALPHA = 0.20f
private const val HERO_COUNTER_CENTER_X = 0.12f
private const val HERO_COUNTER_CENTER_Y = 0.58f
private const val HERO_COUNTER_RADIUS = 0.75f
private const val HERO_COUNTER_ALPHA = 0.45f
private const val SCRIM_TOP_STOP = 0.40f
private const val SCRIM_MID_STOP = 0.68f
private const val SCRIM_MID_ALPHA = 0.55f
private const val SCRIM_BOTTOM_ALPHA = 0.97f

/**
 * Abstract stand-in for the mockup's hero photo slot: a warm terracotta horizon glow and a
 * cool counter-glow over the ink canvas, plus the design's bottom scrim so the brand block
 * and card always sit on near-solid ink.
 */
@Composable
private fun HeroBackdrop(modifier: Modifier = Modifier) {
    val warm = MaterialTheme.colorScheme.primary
    val cool = MaterialTheme.colorScheme.surface
    val ink = MaterialTheme.alongsideColors.gradientTop
    Canvas(modifier = modifier) {
        drawRect(
            brush =
                Brush.radialGradient(
                    colors = listOf(warm.copy(alpha = HERO_GLOW_ALPHA), Color.Transparent),
                    center = Offset(size.width * HERO_GLOW_CENTER_X, size.height * HERO_GLOW_CENTER_Y),
                    radius = size.width * HERO_GLOW_RADIUS,
                ),
        )
        drawRect(
            brush =
                Brush.radialGradient(
                    colors = listOf(cool.copy(alpha = HERO_COUNTER_ALPHA), Color.Transparent),
                    center = Offset(size.width * HERO_COUNTER_CENTER_X, size.height * HERO_COUNTER_CENTER_Y),
                    radius = size.width * HERO_COUNTER_RADIUS,
                ),
        )
        drawRect(
            brush =
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    SCRIM_TOP_STOP to Color.Transparent,
                    SCRIM_MID_STOP to ink.copy(alpha = SCRIM_MID_ALPHA),
                    1f to ink.copy(alpha = SCRIM_BOTTOM_ALPHA),
                ),
        )
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
private fun AuthScreenRestoringPreview() {
    AlongsideTheme {
        AuthContent(
            state = AuthState(isRestoringSession = true),
            onSignInClick = {},
            modifier = PreviewSize,
            animateEntrance = false,
        )
    }
}

@Preview
@Composable
private fun AuthScreenIdlePreview() {
    AlongsideTheme {
        AuthContent(
            state = AuthState(isRestoringSession = false),
            onSignInClick = {},
            modifier = PreviewSize,
            animateEntrance = false,
        )
    }
}

@Preview
@Composable
private fun AuthScreenLoadingPreview() {
    AlongsideTheme {
        AuthContent(
            state = AuthState(isRestoringSession = false, isSigningIn = true),
            onSignInClick = {},
            modifier = PreviewSize,
            animateEntrance = false,
        )
    }
}

@Preview
@Composable
private fun AuthScreenErrorPreview() {
    AlongsideTheme {
        AuthContent(
            state = AuthState(isRestoringSession = false, error = AuthError.NETWORK),
            onSignInClick = {},
            modifier = PreviewSize,
            animateEntrance = false,
        )
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
        AuthContent(
            state = AuthState(isRestoringSession = false, session = session),
            onSignInClick = {},
            modifier = PreviewSize,
            animateEntrance = false,
        )
    }
}
