package com.alongside.core.ui.animation

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StaggerRevealColumnTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `before the full stagger duration the last item is not yet revealed`() {
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            StaggerRevealColumn(itemCount = 4, staggerDelayMillis = 80L) { index ->
                Text("Item $index")
            }
        }

        composeTestRule.mainClock.advanceTimeBy(1L)

        composeTestRule.onNodeWithTag("stagger-item-3").assertDoesNotExist()
    }

    @Test
    fun `after the full stagger duration every item is displayed`() {
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            StaggerRevealColumn(itemCount = 4, staggerDelayMillis = 80L) { index ->
                Text("Item $index")
            }
        }

        composeTestRule.mainClock.advanceTimeBy(4 * 80L + 500L)
        composeTestRule.waitForIdle()

        for (index in 0 until 4) {
            composeTestRule.onNodeWithTag("stagger-item-$index").assertIsDisplayed()
        }
    }
}
