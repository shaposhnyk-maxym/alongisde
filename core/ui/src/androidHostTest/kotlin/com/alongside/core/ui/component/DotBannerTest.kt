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
class DotBannerTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `renders banner text`() {
        composeTestRule.setContent {
            AlongsideTheme {
                DotBanner(text = "Added \"Rynok Square\" from Google Maps")
            }
        }

        composeTestRule.onNodeWithText("Added \"Rynok Square\" from Google Maps").assertIsDisplayed()
    }
}
