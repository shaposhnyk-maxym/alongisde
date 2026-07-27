package com.alongside.feature.matcher.presentation

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.dp
import com.alongside.core.model.place.SwipeDirection
import com.alongside.core.ui.theme.AlongsideTheme
import com.alongside.feature.matcher.fakeCandidate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

/**
 * The idle wobble itself is driven by `rememberInfiniteTransition` (same as `PulsingDot`/
 * `DashedRingBadge` elsewhere in this codebase) specifically because Compose's test framework
 * excludes `InfiniteTransition`-driven animations from its "is the app idle" check - a
 * `graphicsLayer` transform isn't otherwise observable through Compose UI test's semantics tree
 * under Robolectric, and an earlier `LaunchedEffect`+`withFrameMillis`-driven attempt at this same
 * animation made every Espresso-backed test in this module hang. This file only guards that real
 * drag-to-swipe behavior still works with the wobble's `isDragging` gating wired in.
 */
@RunWith(RobolectricTestRunner::class)
class SwipeableCardWobbleTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `a real drag still commits a swipe decision with the idle wobble wired in`() {
        var captured: SwipeDirection? = null
        composeTestRule.setContent {
            AlongsideTheme {
                SwipeableCard(
                    candidate = fakeCandidate("place-1"),
                    onSwipe = { captured = it },
                    modifier = Modifier.size(360.dp, 500.dp),
                )
            }
        }

        composeTestRule.onNodeWithTag("swipeable-card").performTouchInput { swipeRight() }
        composeTestRule.waitForIdle()

        assertEquals(SwipeDirection.LIKE, captured)
    }
}
