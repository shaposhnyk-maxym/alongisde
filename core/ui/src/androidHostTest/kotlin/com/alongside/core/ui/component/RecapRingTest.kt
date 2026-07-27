package com.alongside.core.ui.component

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.alongside.core.ui.theme.AlongsideTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class RecapRingTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `click invokes onClick`() {
        var clicked = false
        composeTestRule.setContent {
            AlongsideTheme {
                RecapRing(onClick = { clicked = true }, contentDescription = "Your recap is ready", pulse = false)
            }
        }

        composeTestRule.onNodeWithContentDescription("Your recap is ready").performClick()

        assertTrue(clicked)
    }
}
