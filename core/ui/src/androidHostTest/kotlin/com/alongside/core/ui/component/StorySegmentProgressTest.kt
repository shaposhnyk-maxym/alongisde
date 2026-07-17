package com.alongside.core.ui.component

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import com.alongside.core.ui.theme.AlongsideTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StorySegmentProgressTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun progressEquals(expected: Float): SemanticsMatcher =
        SemanticsMatcher("progress == $expected") { node ->
            node.config.getOrNull(SemanticsProperties.ProgressBarRangeInfo)?.current == expected
        }

    @Test
    fun `renders one segment per count`() {
        composeTestRule.setContent {
            AlongsideTheme {
                StorySegmentProgress(segmentCount = 4, activeSegment = 1)
            }
        }

        composeTestRule
            .onAllNodesWithTag("story-segment", useUnmergedTree = true)
            .assertCountEquals(4)
    }

    @Test
    fun `active progress above one is coerced to one`() {
        composeTestRule.setContent {
            AlongsideTheme {
                StorySegmentProgress(segmentCount = 3, activeSegment = 0, activeProgress = 1.7f)
            }
        }

        composeTestRule.onNode(progressEquals(1f), useUnmergedTree = true).assertExists()
    }

    @Test
    fun `negative active progress is coerced to zero`() {
        composeTestRule.setContent {
            AlongsideTheme {
                StorySegmentProgress(segmentCount = 3, activeSegment = 2, activeProgress = -0.5f)
            }
        }

        composeTestRule.onNode(progressEquals(0f), useUnmergedTree = true).assertExists()
    }
}
