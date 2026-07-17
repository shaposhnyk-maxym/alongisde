package com.alongside.core.ui.animation

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.alongside.core.ui.theme.AlongsideTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PulsingDotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `renders a dot node`() {
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            AlongsideTheme {
                PulsingDot()
            }
        }

        composeTestRule.onNodeWithTag("pulsing-dot").assertExists()
    }
}
