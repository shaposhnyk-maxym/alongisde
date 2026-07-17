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
class DigitTileTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `renders the character`() {
        composeTestRule.setContent {
            AlongsideTheme {
                DigitTile(char = '7')
            }
        }

        composeTestRule.onNodeWithText("7").assertIsDisplayed()
    }
}
