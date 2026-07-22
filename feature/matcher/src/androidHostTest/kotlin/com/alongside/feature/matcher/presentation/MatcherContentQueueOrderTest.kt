package com.alongside.feature.matcher.presentation

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import com.alongside.feature.matcher.fakeSwipe
import com.alongside.feature.matcher.fakeTrip
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

/**
 * Reproduces the fix for a UX gap found late in M15 planning: a naive `myTurnDeck.firstOrNull()`
 * would show the same card again the instant a swipe creates a split (partner already answered
 * the opposite way). `MatcherContent`'s local queue instead removes it from the front immediately
 * and only re-admits it - at the *end* - once the container confirms it's still my turn.
 */
@RunWith(RobolectricTestRunner::class)
class MatcherContentQueueOrderTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `a split card that reappears rejoins the queue at the bottom, not the front`() {
        val trip = fakeTrip(ownerId = "owner-1", memberId = "member-1")
        val candidates = listOf(fakeCandidate("place-1"), fakeCandidate("place-2"))
        var state by mutableStateOf(MatcherState(ownUserId = "owner-1", trip = trip, candidates = candidates))
        val swiped = mutableListOf<Pair<String, SwipeDirection>>()

        composeTestRule.setContent {
            AlongsideTheme {
                MatcherContent(
                    state = state,
                    onSwipe = { id, direction -> swiped += id to direction },
                    modifier = Modifier.size(360.dp, 640.dp),
                )
            }
        }

        // I dislike place-1 (the front card, fresh order = candidates order).
        composeTestRule.onNodeWithTag("swipeable-card").performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()
        assertEquals(listOf("place-1" to SwipeDirection.DISLIKE), swiped)

        // The container's next emission: my dislike lands alongside the partner's earlier like -
        // a split, so place-1 is still my turn (offered back for reconsideration).
        state =
            state.copy(
                swipes =
                    listOf(
                        fakeSwipe("place-1", "owner-1", SwipeDirection.DISLIKE),
                        fakeSwipe("place-1", "member-1", SwipeDirection.LIKE),
                    ),
            )
        composeTestRule.waitForIdle()

        // place-2 (never touched) should be front now, not place-1 (sent to the back).
        composeTestRule.onNodeWithTag("swipeable-card").performTouchInput { swipeRight() }
        composeTestRule.waitForIdle()

        assertEquals(
            listOf("place-1" to SwipeDirection.DISLIKE, "place-2" to SwipeDirection.LIKE),
            swiped,
        )
    }
}
