package com.alongside.core.ui.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.alongside.core.ui.theme.AlongsideTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OverlineLabelTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `renders text uppercased`() {
        composeTestRule.setContent {
            AlongsideTheme {
                OverlineLabel(text = "Step 1 of 4")
            }
        }

        composeTestRule.onNodeWithText("STEP 1 OF 4").assertIsDisplayed()
    }

    @Test
    fun `accent tone renders uppercased text`() {
        composeTestRule.setContent {
            AlongsideTheme {
                OverlineLabel(text = "Trip Day", tone = OverlineLabelTone.Accent)
            }
        }

        composeTestRule.onNodeWithText("TRIP DAY").assertIsDisplayed()
    }
}
