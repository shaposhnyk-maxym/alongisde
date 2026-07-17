package com.alongside.core.ui.animation

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class TypewriterTextTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val fullText = "Your trip, together"

    @Test
    fun `after completion the full text is shown, not an intermediate one`() {
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            TypewriterText(text = fullText, charDelayMillis = 20L)
        }

        composeTestRule.mainClock.advanceTimeBy(fullText.length * 20L + 200L)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(fullText).assertExists()
    }

    @Test
    fun `before completion only a partial text is shown`() {
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            TypewriterText(text = fullText, charDelayMillis = 20L)
        }

        composeTestRule.mainClock.advanceTimeBy(40L)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(fullText).assertDoesNotExist()
    }

    @Test
    fun `blinking cursor is appended to the partial text while revealing`() {
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            TypewriterText(text = fullText, charDelayMillis = 20L, showCursor = true)
        }

        composeTestRule.mainClock.advanceTimeBy(80L)
        composeTestRule.waitForIdle()

        // Frame timing decides exactly how many chars are visible; the invariant is a
        // non-empty partial text with the trailing cursor.
        val partialWithCursor =
            SemanticsMatcher("partial text with trailing cursor") { node ->
                val texts = node.config.getOrNull(SemanticsProperties.Text)?.map { it.text }.orEmpty()
                texts.any { it.endsWith("_") && it.length in 2 until fullText.length + 1 }
            }
        composeTestRule.onNode(partialWithCursor).assertExists()
    }

    @Test
    fun `cursor keeps blinking after the text is fully revealed`() {
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            TypewriterText(text = fullText, charDelayMillis = 20L, showCursor = true)
        }

        // Reveal finishes at 380ms; the cursor toggles off at 500ms...
        composeTestRule.mainClock.advanceTimeBy(700L)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(fullText).assertExists()

        // ...and back on at 1000ms.
        composeTestRule.mainClock.advanceTimeBy(500L)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(fullText + "_").assertExists()
    }

    @Test
    fun `onComplete fires once the full text is revealed`() {
        composeTestRule.mainClock.autoAdvance = false
        var completed = false

        composeTestRule.setContent {
            TypewriterText(text = fullText, charDelayMillis = 20L, onComplete = { completed = true })
        }

        composeTestRule.mainClock.advanceTimeBy(fullText.length * 20L + 200L)
        composeTestRule.waitForIdle()

        assertEquals(true, completed)
    }
}
