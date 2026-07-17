package com.alongside.core.ui.component

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.alongside.core.ui.theme.AlongsideTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@RunWith(RobolectricTestRunner::class)
class AlongsideButtonTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `click on primary button invokes onClick`() {
        var clicked = false
        composeTestRule.setContent {
            AlongsideTheme {
                AlongsidePrimaryButton(text = "Continue", onClick = { clicked = true })
            }
        }

        composeTestRule.onNodeWithText("Continue").performClick()

        assertEquals(true, clicked)
    }

    @Test
    fun `disabled primary button does not invoke onClick`() {
        var clicked = false
        composeTestRule.setContent {
            AlongsideTheme {
                AlongsidePrimaryButton(text = "Continue", onClick = { clicked = true }, enabled = false)
            }
        }

        composeTestRule.onNodeWithText("Continue").performClick()

        assertFalse(clicked)
    }

    @Test
    fun `click on secondary button invokes onClick`() {
        var clicked = false
        composeTestRule.setContent {
            AlongsideTheme {
                AlongsideSecondaryButton(text = "Join a Trip", onClick = { clicked = true })
            }
        }

        composeTestRule.onNodeWithText("Join a Trip").performClick()

        assertEquals(true, clicked)
    }

    @Test
    fun `click on onPaper button invokes onClick`() {
        var clicked = false
        composeTestRule.setContent {
            AlongsideTheme {
                AlongsideOnPaperButton(text = "Continue with Google", onClick = { clicked = true })
            }
        }

        composeTestRule.onNodeWithText("Continue with Google").performClick()

        assertEquals(true, clicked)
    }

    @Test
    fun `disabled onPaper button does not invoke onClick`() {
        var clicked = false
        composeTestRule.setContent {
            AlongsideTheme {
                AlongsideOnPaperButton(text = "Continue with Google", onClick = { clicked = true }, enabled = false)
            }
        }

        composeTestRule.onNodeWithText("Continue with Google").performClick()

        assertFalse(clicked)
    }

    @Test
    fun `click on text button invokes onClick`() {
        var clicked = false
        composeTestRule.setContent {
            AlongsideTheme {
                AlongsideTextButton(text = "Not now", onClick = { clicked = true })
            }
        }

        composeTestRule.onNodeWithText("Not now").performClick()

        assertEquals(true, clicked)
    }
}
