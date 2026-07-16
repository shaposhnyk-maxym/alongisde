package com.alongside.core.ui.animation

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
