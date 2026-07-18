package com.alongside.feature.pairing.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.alongside.core.domain.pairing.INVITE_CODE_LENGTH
import com.alongside.core.ui.animation.FadeUpReveal
import com.alongside.core.ui.animation.PulsingDot
import com.alongside.core.ui.component.AlongsidePrimaryButton
import com.alongside.core.ui.component.AlongsideSecondaryButton
import com.alongside.core.ui.component.AlongsideTextButton
import com.alongside.core.ui.component.DigitTile
import com.alongside.core.ui.component.DotBanner
import com.alongside.core.ui.component.OverlineLabel
import com.alongside.core.ui.component.OverlineLabelTone
import com.alongside.core.ui.component.PaperCard
import com.alongside.core.ui.theme.AlongsideSpacing
import com.alongside.core.ui.theme.alongsideColors

private const val CARD_REVEAL_DELAY_MILLIS = 100L
private val StepScreenTopPadding = AlongsideSpacing.xxl
private val StepScreenBottomPadding = AlongsideSpacing.xxl

@Composable
private fun StepColumn(content: @Composable ColumnScope.() -> Unit) {
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
        content = content,
    )
}

// ─── Choice · create or join ──────────────────────────────────────────────────

@Composable
internal fun ChoiceStep(
    isCreating: Boolean,
    onCreateTrip: () -> Unit,
    onStartJoinFlow: () -> Unit,
    animateEntrance: Boolean,
) {
    StepColumn {
        OverlineLabel(text = "Pairing", tone = OverlineLabelTone.Accent)
        Spacer(Modifier.height(AlongsideSpacing.xxl))
        FadeUpReveal(
            delayMillis = CARD_REVEAL_DELAY_MILLIS,
            initiallyRevealed = !animateEntrance,
        ) {
            PaperCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Every trip needs two",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(AlongsideSpacing.md))
                Text(
                    text =
                        "Create a trip and share the invite code, " +
                            "or join with the code your partner sent you.",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.alongsideColors.onPaperSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Spacer(Modifier.weight(1f))
        AlongsidePrimaryButton(
            text = "Create a Trip",
            onClick = onCreateTrip,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isCreating,
        )
        Spacer(Modifier.height(AlongsideSpacing.md))
        AlongsideSecondaryButton(
            text = "Join with a Code",
            onClick = onStartJoinFlow,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isCreating,
        )
    }
}

// ─── Create · show the code, wait for the partner ─────────────────────────────

@Composable
internal fun CreateShowCodeStep(
    inviteCode: String,
    animateEntrance: Boolean,
) {
    StepColumn {
        OverlineLabel(text = "Your invite code", tone = OverlineLabelTone.Accent)
        Spacer(Modifier.height(AlongsideSpacing.xxl))
        Row(horizontalArrangement = Arrangement.spacedBy(AlongsideSpacing.sm)) {
            inviteCode.forEach { char -> DigitTile(char = char) }
        }
        Spacer(Modifier.height(AlongsideSpacing.xxl))
        FadeUpReveal(
            delayMillis = CARD_REVEAL_DELAY_MILLIS,
            initiallyRevealed = !animateEntrance,
        ) {
            PaperCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Share this code",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(AlongsideSpacing.md))
                Text(
                    text =
                        "Read it aloud, text it, send a pigeon - " +
                            "as soon as your partner enters it, you're paired.",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.alongsideColors.onPaperSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AlongsideSpacing.md),
        ) {
            PulsingDot()
            Text(
                text = "Waiting for your partner…",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

// ─── Join · enter the partner's code ──────────────────────────────────────────

@Composable
internal fun JoinEnterCodeStep(
    codeInput: String,
    isJoining: Boolean,
    joinError: PairingJoinError?,
    onBackToChoice: () -> Unit,
    onCodeInputChanged: (String) -> Unit,
    onSubmitCode: () -> Unit,
) {
    StepColumn {
        OverlineLabel(text = "Join a trip", tone = OverlineLabelTone.Accent)
        Spacer(Modifier.height(AlongsideSpacing.xxl))
        // No FadeUpReveal here: AnimatedVisibility doesn't compose unrevealed content, and the
        // code input must be interactable immediately (same reason onboarding keeps its action
        // buttons outside the reveal).
        PaperCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Enter your partner's code",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(AlongsideSpacing.md))
            OutlinedTextField(
                value = codeInput,
                onValueChange = onCodeInputChanged,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .testTag("pairing-code-input"),
                placeholder = { Text("ABCD23") },
                singleLine = true,
                // OutlinedTextField reads colorScheme tokens tuned for the dark ink
                // background - on the cream PaperCard they render cream-on-cream.
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.alongsideColors.onPaper,
                        unfocusedTextColor = MaterialTheme.alongsideColors.onPaper,
                        focusedPlaceholderColor = MaterialTheme.alongsideColors.onPaperSecondary,
                        unfocusedPlaceholderColor = MaterialTheme.alongsideColors.onPaperSecondary,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.alongsideColors.onPaperSecondary,
                    ),
            )
        }
        joinError?.let { error ->
            Spacer(Modifier.height(AlongsideSpacing.lg))
            DotBanner(
                text = error.message(),
                modifier =
                    Modifier.semantics {
                        contentDescription = "pairing-join-error-${error.name}"
                    },
            )
        }
        Spacer(Modifier.weight(1f))
        AlongsidePrimaryButton(
            text = "Join Trip",
            onClick = onSubmitCode,
            modifier = Modifier.fillMaxWidth(),
            enabled = codeInput.length == INVITE_CODE_LENGTH && !isJoining,
        )
        Spacer(Modifier.height(AlongsideSpacing.md))
        AlongsideTextButton(
            text = "Back",
            onClick = onBackToChoice,
        )
    }
}

private fun PairingJoinError.message(): String =
    when (this) {
        PairingJoinError.INVALID_CODE -> "That code doesn't match any trip. Double-check it and try again."
        PairingJoinError.OWN_CODE -> "That's your own code - share it with your partner instead."
        PairingJoinError.ALREADY_USED -> "That code has already been used by someone else."
    }
