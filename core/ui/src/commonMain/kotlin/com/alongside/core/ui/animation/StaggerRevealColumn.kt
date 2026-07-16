package com.alongside.core.ui.animation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alongside.core.ui.component.PaperCard
import com.alongside.core.ui.theme.AlongsideTheme
import kotlinx.coroutines.delay

/**
 * Reveals `itemCount` items one at a time, [staggerDelayMillis] apart, each mounting via
 * [AnimatedVisibility] (not just an alpha fade) so its final "revealed" state is directly
 * queryable in tests.
 */
@Composable
public fun StaggerRevealColumn(
    itemCount: Int,
    modifier: Modifier = Modifier,
    staggerDelayMillis: Long = 80L,
    itemTestTag: (Int) -> String = { "stagger-item-$it" },
    content: @Composable (index: Int) -> Unit,
) {
    var revealedCount by remember(itemCount) { mutableIntStateOf(0) }

    LaunchedEffect(itemCount, staggerDelayMillis) {
        revealedCount = 0
        repeat(itemCount) {
            delay(staggerDelayMillis)
            revealedCount++
        }
    }

    Column(modifier = modifier) {
        for (index in 0 until itemCount) {
            AnimatedVisibility(
                visible = index < revealedCount,
                enter = fadeIn() + slideInVertically(),
                modifier = Modifier.testTag(itemTestTag(index)),
            ) {
                content(index)
            }
        }
    }
}

@Preview
@Composable
private fun StaggerRevealColumnPreview() {
    AlongsideTheme {
        // Fixed size so the screenshot capture has a non-empty root even at the animation's
        // very first (nothing-revealed-yet) frame.
        StaggerRevealColumn(itemCount = 3, modifier = Modifier.size(240.dp, 200.dp)) { index ->
            PaperCard { Text("Item $index") }
        }
    }
}
