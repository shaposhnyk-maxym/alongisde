package com.alongside.core.ui.animation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alongside.core.ui.component.PaperCard
import com.alongside.core.ui.theme.AlongsideTheme
import kotlinx.coroutines.delay

private const val REVEAL_DURATION_MILLIS = 700
private const val SLIDE_OFFSET_FRACTION = 8

/**
 * The design's `fadeUp` entrance: content fades in while sliding up a small fraction of its
 * height, optionally after [delayMillis]. Mounts via [AnimatedVisibility] (same reasoning as
 * [StaggerRevealColumn]) so the revealed state is queryable in tests. [initiallyRevealed]
 * renders the settled end state on the first frame - previews/screenshots use it so goldens
 * capture the finished layout instead of the pre-reveal blank frame.
 */
@Composable
public fun FadeUpReveal(
    modifier: Modifier = Modifier,
    delayMillis: Long = 0L,
    initiallyRevealed: Boolean = false,
    content: @Composable () -> Unit,
) {
    val visibleState = remember { MutableTransitionState(initiallyRevealed) }
    LaunchedEffect(Unit) {
        delay(delayMillis)
        visibleState.targetState = true
    }
    AnimatedVisibility(
        visibleState = visibleState,
        modifier = modifier.testTag("fade-up-reveal"),
        enter =
            fadeIn(tween(REVEAL_DURATION_MILLIS)) +
                slideInVertically(tween(REVEAL_DURATION_MILLIS)) { it / SLIDE_OFFSET_FRACTION },
    ) {
        content()
    }
}

@Preview
@Composable
private fun FadeUpRevealPreview() {
    AlongsideTheme {
        // initiallyRevealed so the screenshot captures the settled state, not the blank
        // pre-reveal frame; fixed size so the capture root is never empty either way.
        FadeUpReveal(initiallyRevealed = true, modifier = Modifier.size(240.dp, 120.dp)) {
            PaperCard { Text("Revealed") }
        }
    }
}
