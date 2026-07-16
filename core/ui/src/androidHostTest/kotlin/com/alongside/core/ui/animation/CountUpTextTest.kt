package com.alongside.core.ui.animation

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CountUpTextTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `after the animation completes the target value is shown`() {
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            CountUpText(targetValue = 7, durationMillis = 800)
        }

        composeTestRule.mainClock.advanceTimeBy(1_000L)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("7").assertExists()
    }

    @Test
    fun `before the animation completes the target value is not yet shown`() {
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            CountUpText(targetValue = 100, startValue = 0, durationMillis = 800)
        }

        composeTestRule.mainClock.advanceTimeBy(1L)

        composeTestRule.onNodeWithText("100").assertDoesNotExist()
    }
}
