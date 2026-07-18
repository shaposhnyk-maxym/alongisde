package com.alongside.feature.pairing.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.alongside.core.ui.component.InkGradientBackground
import org.orbitmvi.orbit.compose.collectAsState

@Composable
public fun PairingScreen(
    container: PairingContainer,
    modifier: Modifier = Modifier,
) {
    val state by container.collectAsState()

    PairingContent(
        state = state,
        onCreateTrip = { container.onIntent(PairingIntent.CreateTrip) },
        onStartJoinFlow = { container.onIntent(PairingIntent.StartJoinFlow) },
        onBackToChoice = { container.onIntent(PairingIntent.BackToChoice) },
        onCodeInputChange = { container.onIntent(PairingIntent.CodeInputChanged(it)) },
        onSubmitCode = { container.onIntent(PairingIntent.SubmitCode) },
        modifier = modifier,
    )
}

@Composable
internal fun PairingContent(
    state: PairingState,
    onCreateTrip: () -> Unit,
    onStartJoinFlow: () -> Unit,
    onBackToChoice: () -> Unit,
    onCodeInputChange: (String) -> Unit,
    onSubmitCode: () -> Unit,
    modifier: Modifier = Modifier,
    animateEntrance: Boolean = true,
) {
    InkGradientBackground(modifier = modifier.fillMaxSize()) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .semantics {
                        contentDescription = "pairing-step-${state.step.name}"
                    },
        ) {
            when (state.step) {
                PairingStep.CHOICE ->
                    ChoiceStep(
                        isCreating = state.isCreating,
                        onCreateTrip = onCreateTrip,
                        onStartJoinFlow = onStartJoinFlow,
                        animateEntrance = animateEntrance,
                    )
                PairingStep.CREATE_SHOW_CODE ->
                    state.ownTrip?.let { trip ->
                        CreateShowCodeStep(
                            inviteCode = trip.inviteCode,
                            animateEntrance = animateEntrance,
                        )
                    }
                PairingStep.JOIN_ENTER_CODE ->
                    JoinEnterCodeStep(
                        codeInput = state.codeInput,
                        isJoining = state.isJoining,
                        joinError = state.joinError,
                        onBackToChoice = onBackToChoice,
                        onCodeInputChange = onCodeInputChange,
                        onSubmitCode = onSubmitCode,
                    )
            }
        }
    }
}
