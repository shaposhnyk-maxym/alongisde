package com.alongside.feature.onboarding.presentation

import alongside.feature.onboarding.generated.resources.Res
import alongside.feature.onboarding.generated.resources.ic_camera
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alongside.core.ui.animation.FadeUpReveal
import com.alongside.core.ui.component.AlongsidePrimaryButton
import com.alongside.core.ui.component.AlongsideSecondaryButton
import com.alongside.core.ui.component.AlongsideTextButton
import com.alongside.core.ui.component.DotBanner
import com.alongside.core.ui.component.OverlineLabel
import com.alongside.core.ui.component.OverlineLabelTone
import com.alongside.core.ui.component.PaperCard
import com.alongside.core.ui.theme.AlongsideSpacing
import com.alongside.core.ui.theme.alongsideColors
import com.alongside.core.ui.theme.alongsideTypography
import com.alongside.feature.onboarding.OnboardingStep
import com.alongside.feature.onboarding.PermissionStatus
import org.jetbrains.compose.resources.painterResource

private const val STEP_COUNT = 4
private const val CARD_REVEAL_DELAY_MILLIS = 100L
private val StepScreenTopPadding = 40.dp
private val StepScreenBottomPadding = 40.dp

@Composable
private fun StepLabel(step: OnboardingStep) {
    OverlineLabel(
        text = "Step ${step.ordinal + 1} of $STEP_COUNT",
        tone = OverlineLabelTone.Accent,
    )
}

// ─── Step 1 · Photo access ────────────────────────────────────────────────────

private val IconTileSize = 64.dp
private val IconTileCorner = 18.dp
private val CameraIconWidth = 30.dp
private val CameraIconHeight = 22.dp
private val IconTileGap = 28.dp

@Composable
internal fun PhotoPermissionStep(
    status: PermissionStatus,
    onRequest: () -> Unit,
    onOpenAppSettings: () -> Unit,
    animateEntrance: Boolean,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(
                    start = AlongsideSpacing.xxl,
                    end = AlongsideSpacing.xxl,
                    top = StepScreenTopPadding,
                    bottom = StepScreenBottomPadding,
                ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        StepLabel(OnboardingStep.PHOTO_PERMISSION)
        Spacer(Modifier.height(IconTileGap))
        Box(
            modifier =
                Modifier
                    .size(IconTileSize)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(IconTileCorner)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_camera),
                contentDescription = null,
                modifier = Modifier.size(CameraIconWidth, CameraIconHeight),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.height(IconTileGap))
        FadeUpReveal(
            delayMillis = CARD_REVEAL_DELAY_MILLIS,
            initiallyRevealed = !animateEntrance,
        ) {
            PaperCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Let your trip write itself",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(AlongsideSpacing.md))
                Text(
                    text =
                        "Alongside turns your camera roll into a shared diary. " +
                            "Allow access to your photos to get started.",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.alongsideColors.onPaperSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Spacer(Modifier.weight(1f))
        PermissionActions(
            status = status,
            allowLabel = "Allow Photo Access",
            onRequest = onRequest,
            onOpenAppSettings = onOpenAppSettings,
        )
    }
}

// ─── Step 2 · Location for the camera ─────────────────────────────────────────

private val MockAvatarSize = 40.dp
private val MockRowStartPadding = 56.dp

@Composable
internal fun CameraGeolocationStep(
    onAcknowledge: () -> Unit,
    onOpenAppSettings: () -> Unit,
    animateEntrance: Boolean,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(
                    start = AlongsideSpacing.xxl,
                    end = AlongsideSpacing.xxl,
                    top = StepScreenTopPadding,
                    bottom = AlongsideSpacing.xxl,
                ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        StepLabel(OnboardingStep.CAMERA_GEOLOCATION)
        Spacer(Modifier.height(AlongsideSpacing.xxl))
        SettingsMock()
        Spacer(Modifier.weight(1f))
        FadeUpReveal(
            delayMillis = CARD_REVEAL_DELAY_MILLIS,
            initiallyRevealed = !animateEntrance,
        ) {
            PaperCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Turn on location for the camera",
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(Modifier.height(AlongsideSpacing.sm))
                Text(
                    text =
                        "Android keeps camera location separate from app location. " +
                            "Both need to be on for your diary to cluster photos automatically.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.alongsideColors.onPaperSecondary,
                )
                Spacer(Modifier.height(AlongsideSpacing.md))
                Text(
                    text =
                        "Settings → Apps → Alongside →\n" +
                            "Permissions → Location →\n" +
                            "Allow only while using the app",
                    style = MaterialTheme.alongsideTypography.meta.copy(lineHeight = 21.sp),
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(AlongsideSpacing.lg))
                AlongsidePrimaryButton(
                    text = "Open Settings",
                    onClick = onOpenAppSettings,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Spacer(Modifier.height(AlongsideSpacing.sm))
        AlongsideTextButton(text = "Got It", onClick = onAcknowledge)
    }
}

/** Non-interactive mock of the system App-info permissions screen the step points to. */
@Composable
private fun SettingsMock(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.alongsideColors.paper,
        contentColor = MaterialTheme.alongsideColors.onPaper,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(modifier = Modifier.padding(AlongsideSpacing.lg)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AlongsideSpacing.lg),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(MockAvatarSize)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "A",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.alongsideColors.paperWhite,
                    )
                }
                Text(text = "Permissions", style = MaterialTheme.typography.bodyLarge)
            }
            Spacer(Modifier.height(AlongsideSpacing.md))
            SettingsMockRow(name = "Location", status = "Only while using app")
            SettingsMockRow(name = "Photos and videos", status = "Allowed")
        }
    }
}

@Composable
private fun SettingsMockRow(
    name: String,
    status: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = MockRowStartPadding, top = AlongsideSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = name, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.weight(1f))
        Text(
            text = status,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

// ─── Step 3 · Share from Google Maps ──────────────────────────────────────────

private val ShareSheetCorner = 24.dp
private val ShareBodyMaxWidth = 300.dp
private val ShareSheetTopPadding = 18.dp
private val ShareSheetBottomPadding = 28.dp
private const val SHARE_HIGHLIGHT_ALPHA = 0.1f

@Composable
internal fun ShareSetupStep(
    onContinue: () -> Unit,
    animateEntrance: Boolean,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        start = AlongsideSpacing.xxl,
                        end = AlongsideSpacing.xxl,
                        top = StepScreenTopPadding,
                    ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            StepLabel(OnboardingStep.SHARE_SETUP)
            Spacer(Modifier.height(AlongsideSpacing.lg))
            Text(
                text = "Send us a place from Maps",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(AlongsideSpacing.sm))
            Text(
                text = "Tap Share on any place card, then choose Alongside from the list.",
                modifier = Modifier.widthIn(max = ShareBodyMaxWidth),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(AlongsideSpacing.xxl))
            AlongsidePrimaryButton(
                text = "Continue",
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.weight(1f))
        FadeUpReveal(
            delayMillis = CARD_REVEAL_DELAY_MILLIS,
            initiallyRevealed = !animateEntrance,
        ) {
            ShareSheetMock(modifier = Modifier.fillMaxWidth())
        }
    }
}

/** Non-interactive mock of the Android share sheet with Alongside highlighted. */
@Composable
private fun ShareSheetMock(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.alongsideColors.paperWhite,
        contentColor = MaterialTheme.alongsideColors.onPaper,
        shape = RoundedCornerShape(topStart = ShareSheetCorner, topEnd = ShareSheetCorner),
    ) {
        Column(
            modifier =
                Modifier
                    .navigationBarsPadding()
                    .padding(
                        start = AlongsideSpacing.sm,
                        end = AlongsideSpacing.sm,
                        top = ShareSheetTopPadding,
                        bottom = ShareSheetBottomPadding,
                    ),
        ) {
            Text(
                text = "Share with",
                modifier = Modifier.padding(start = AlongsideSpacing.lg, bottom = AlongsideSpacing.md),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.alongsideColors.onPaperSecondary,
            )
            ShareTargetRow(name = "Alongside", highlighted = true)
            ShareTargetRow(name = "Messages")
            ShareTargetRow(name = "Gmail")
        }
    }
}

@Composable
private fun ShareTargetRow(
    name: String,
    highlighted: Boolean = false,
) {
    val rowModifier =
        if (highlighted) {
            Modifier.background(
                MaterialTheme.colorScheme.primary.copy(alpha = SHARE_HIGHLIGHT_ALPHA),
                MaterialTheme.shapes.small,
            )
        } else {
            Modifier
        }
    Row(
        modifier =
            rowModifier
                .fillMaxWidth()
                .padding(horizontal = AlongsideSpacing.lg, vertical = AlongsideSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AlongsideSpacing.lg),
    ) {
        Box(
            modifier =
                Modifier
                    .size(MockAvatarSize)
                    .background(
                        if (highlighted) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.alongsideColors.iconTileOnPaper
                        },
                        CircleShape,
                    ),
        )
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (highlighted) FontWeight.SemiBold else null,
        )
    }
}

// ─── Step 4 · Notifications ───────────────────────────────────────────────────

private val GhostRowHeight = 70.dp
private val GhostRowCorner = 16.dp
private val GhostRowsTopPadding = 100.dp
private const val GHOST_ROW_ALPHA = 0.35f
private const val DIM_ALPHA = 0.5f
private val DialogCorner = 18.dp
private val DialogHorizontalInset = 38.dp

@Composable
internal fun NotificationPermissionStep(
    status: PermissionStatus,
    onRequest: () -> Unit,
    onOpenAppSettings: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = AlongsideSpacing.xxl)
                    .padding(top = GhostRowsTopPadding)
                    .alpha(GHOST_ROW_ALPHA),
            verticalArrangement = Arrangement.spacedBy(AlongsideSpacing.md),
        ) {
            repeat(2) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(GhostRowHeight)
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(GhostRowCorner)),
                )
            }
        }
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.alongsideColors.gradientTop.copy(alpha = DIM_ALPHA)),
        )
        Box(
            modifier = Modifier.fillMaxWidth().padding(top = StepScreenTopPadding),
            contentAlignment = Alignment.Center,
        ) {
            StepLabel(OnboardingStep.NOTIFICATION_PERMISSION)
        }
        // Deliberately not wrapped in FadeUpReveal: the dialog carries the step's CTA, and the
        // entrance delay would leave the button unmounted when UI tests click right after the
        // step appears. The dim overlay already sells the dialog-popping-in moment.
        Box(
            modifier = Modifier.align(Alignment.Center).padding(horizontal = DialogHorizontalInset),
        ) {
            NotificationDialogMock(
                showAllowAction = status == PermissionStatus.NOT_DETERMINED,
                onAllow = onRequest,
            )
        }
        if (status == PermissionStatus.DENIED || status == PermissionStatus.DENIED_PERMANENTLY) {
            Column(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(
                            start = AlongsideSpacing.xxl,
                            end = AlongsideSpacing.xxl,
                            bottom = StepScreenBottomPadding,
                        ),
            ) {
                PermissionActions(
                    status = status,
                    allowLabel = "Allow Notifications",
                    onRequest = onRequest,
                    onOpenAppSettings = onOpenAppSettings,
                )
            }
        }
    }
}

/** In-app pre-prompt styled after the system notification dialog from the design. */
@Composable
private fun NotificationDialogMock(
    showAllowAction: Boolean,
    onAllow: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(DialogCorner),
    ) {
        Column {
            Column(
                modifier =
                    Modifier.padding(
                        start = AlongsideSpacing.xl,
                        end = AlongsideSpacing.xl,
                        top = AlongsideSpacing.xl,
                        bottom = AlongsideSpacing.lg,
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AlongsideSpacing.sm),
            ) {
                Text(
                    text = "“Alongside” would like to send you notifications",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text =
                        "We'll let you know when your shared day unlocks, " +
                            "and when it's almost time to meet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            if (showAllowAction) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                TextButton(
                    onClick = onAllow,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(0.dp),
                ) {
                    Text(
                        text = "Allow Notifications",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

// ─── Shared pieces ────────────────────────────────────────────────────────────

@Composable
private fun PermissionActions(
    status: PermissionStatus,
    allowLabel: String,
    onRequest: () -> Unit,
    onOpenAppSettings: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AlongsideSpacing.lg)) {
        when (status) {
            PermissionStatus.GRANTED -> Unit
            PermissionStatus.NOT_DETERMINED ->
                AlongsidePrimaryButton(text = allowLabel, onClick = onRequest, modifier = Modifier.fillMaxWidth())
            PermissionStatus.DENIED -> {
                DotBanner(text = "We still need this to work - you can try again.")
                AlongsidePrimaryButton(text = "Try Again", onClick = onRequest, modifier = Modifier.fillMaxWidth())
            }
            // The OS refuses to show the dialog again once permanently denied - request() would
            // silently no-op, so the only way forward is the system Settings screen.
            PermissionStatus.DENIED_PERMANENTLY -> {
                DotBanner(text = "This was turned off. You can turn it back on from Settings.")
                AlongsideSecondaryButton(
                    text = "Open Settings",
                    onClick = onOpenAppSettings,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
