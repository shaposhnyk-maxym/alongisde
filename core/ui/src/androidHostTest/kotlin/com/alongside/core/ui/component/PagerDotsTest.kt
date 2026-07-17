package com.alongside.core.ui.component

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.alongside.core.ui.theme.AlongsideTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PagerDotsTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `renders one dot per page`() {
        composeTestRule.setContent {
            AlongsideTheme {
                PagerDots(pageCount = 5, selectedPage = 0)
            }
        }

        val hasPagerDotTag =
            SemanticsMatcher("testTag starts with pager-dot") { node ->
                node.config.getOrNull(SemanticsProperties.TestTag)?.startsWith("pager-dot") == true
            }
        composeTestRule
            .onAllNodes(hasPagerDotTag, useUnmergedTree = true)
            .assertCountEquals(5)
    }

    @Test
    fun `selected dot is an elongated pill`() {
        composeTestRule.setContent {
            AlongsideTheme {
                PagerDots(pageCount = 3, selectedPage = 1)
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag("pager-dot-1", useUnmergedTree = true)
            .assertWidthIsEqualTo(18.dp)
        composeTestRule
            .onNodeWithTag("pager-dot-0", useUnmergedTree = true)
            .assertWidthIsEqualTo(6.dp)
    }
}
