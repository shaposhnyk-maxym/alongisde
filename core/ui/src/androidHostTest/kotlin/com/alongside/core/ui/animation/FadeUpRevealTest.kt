package com.alongside.core.ui.animation

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FadeUpRevealTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `content stays hidden until the reveal delay elapses`() {
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            FadeUpReveal(delayMillis = 200L) {
                Text("Revealed")
            }
        }

        composeTestRule.mainClock.advanceTimeBy(1L)

        composeTestRule.onNodeWithText("Revealed").assertDoesNotExist()
    }

    @Test
    fun `content is displayed after the delay plus the reveal animation`() {
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            FadeUpReveal(delayMillis = 200L) {
                Text("Revealed")
            }
        }

        composeTestRule.mainClock.advanceTimeBy(200L + 700L + 500L)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Revealed").assertIsDisplayed()
    }

    @Test
    fun `initiallyRevealed renders the content on the very first frame`() {
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            FadeUpReveal(initiallyRevealed = true) {
                Text("Revealed")
            }
        }

        composeTestRule.mainClock.advanceTimeBy(1L)

        composeTestRule.onNodeWithText("Revealed").assertIsDisplayed()
    }
}
