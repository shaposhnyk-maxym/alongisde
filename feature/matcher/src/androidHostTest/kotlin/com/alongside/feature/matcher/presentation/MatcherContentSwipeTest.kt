package com.alongside.feature.matcher.presentation

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.dp
import com.alongside.core.model.place.SwipeDirection
import com.alongside.core.ui.theme.AlongsideTheme
import com.alongside.feature.matcher.fakeCandidate
import com.alongside.feature.matcher.fakeTrip
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class MatcherContentSwipeTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `swiping right emits LIKE for the top candidate`() {
        var captured: Pair<String, SwipeDirection>? = null
        composeTestRule.setContent {
            AlongsideTheme {
                MatcherContent(
                    state =
                        MatcherState(
                            ownUserId = "owner-1",
                            trip = fakeTrip(),
                            candidates = listOf(fakeCandidate("place-1")),
                        ),
                    onSwipe = { id, direction -> captured = id to direction },
                    modifier = Modifier.size(360.dp, 640.dp),
                )
            }
        }

        composeTestRule.onNodeWithTag("swipeable-card").performTouchInput { swipeRight() }
        composeTestRule.waitForIdle()

        assertEquals("place-1" to SwipeDirection.LIKE, captured)
    }

    @Test
    fun `swiping left emits DISLIKE for the top candidate`() {
        var captured: Pair<String, SwipeDirection>? = null
        composeTestRule.setContent {
            AlongsideTheme {
                MatcherContent(
                    state =
                        MatcherState(
                            ownUserId = "owner-1",
                            trip = fakeTrip(),
                            candidates = listOf(fakeCandidate("place-1")),
                        ),
                    onSwipe = { id, direction -> captured = id to direction },
                    modifier = Modifier.size(360.dp, 640.dp),
                )
            }
        }

        composeTestRule.onNodeWithTag("swipeable-card").performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()

        assertEquals("place-1" to SwipeDirection.DISLIKE, captured)
    }
}
