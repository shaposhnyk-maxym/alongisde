package com.alongside.core.ui.component

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.alongside.core.ui.theme.AlongsideTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@RunWith(RobolectricTestRunner::class)
class CircleIconButtonTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `click invokes onClick`() {
        var clicked = false
        composeTestRule.setContent {
            AlongsideTheme {
                CircleIconButton(
                    onClick = { clicked = true },
                    contentDescription = "Skip place",
                ) {
                    Text("✕")
                }
            }
        }

        composeTestRule.onNodeWithContentDescription("Skip place").performClick()

        assertEquals(true, clicked)
    }

    @Test
    fun `disabled button does not invoke onClick`() {
        var clicked = false
        composeTestRule.setContent {
            AlongsideTheme {
                CircleIconButton(
                    onClick = { clicked = true },
                    contentDescription = "Want to go",
                    style = CircleIconButtonStyle.Primary,
                    enabled = false,
                ) {
                    Text("♥")
                }
            }
        }

        composeTestRule.onNodeWithContentDescription("Want to go").performClick()

        assertFalse(clicked)
    }

    @Test
    fun `an explicit size overrides the style default`() {
        composeTestRule.setContent {
            AlongsideTheme {
                CircleIconButton(
                    onClick = {},
                    contentDescription = "Skip place",
                    style = CircleIconButtonStyle.Dark,
                    size = 56.dp,
                ) {
                    Text("✕")
                }
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Skip place")
            .assertWidthIsEqualTo(56.dp)
            .assertHeightIsEqualTo(56.dp)
    }
}
