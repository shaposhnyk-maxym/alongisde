package com.alongside.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.alongside.core.ui.theme.AlongsideTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class StoriesChromeTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `tapping the right zone advances the index by one`() {
        var activeIndex by mutableIntStateOf(0)

        composeTestRule.setContent {
            AlongsideTheme {
                StoriesChrome(
                    slideCount = 3,
                    activeIndex = activeIndex,
                    onActiveIndexChange = { activeIndex = it },
                    autoAdvance = false,
                    modifier = Modifier.fillMaxSize(),
                ) { index, _ ->
                    Text("Slide $index")
                }
            }
        }

        composeTestRule.onNodeWithTag("stories-tap-right").performClick()

        assertEquals(1, activeIndex)
    }

    @Test
    fun `tapping the left zone on the first slide is a no-op`() {
        var activeIndex by mutableIntStateOf(0)

        composeTestRule.setContent {
            AlongsideTheme {
                StoriesChrome(
                    slideCount = 3,
                    activeIndex = activeIndex,
                    onActiveIndexChange = { activeIndex = it },
                    autoAdvance = false,
                    modifier = Modifier.fillMaxSize(),
                ) { index, _ ->
                    Text("Slide $index")
                }
            }
        }

        composeTestRule.onNodeWithTag("stories-tap-left").performClick()

        assertEquals(0, activeIndex)
    }

    @Test
    fun `tapping the left zone after moving right returns to the previous index`() {
        var activeIndex by mutableIntStateOf(1)

        composeTestRule.setContent {
            AlongsideTheme {
                StoriesChrome(
                    slideCount = 3,
                    activeIndex = activeIndex,
                    onActiveIndexChange = { activeIndex = it },
                    autoAdvance = false,
                    modifier = Modifier.fillMaxSize(),
                ) { index, _ ->
                    Text("Slide $index")
                }
            }
        }

        composeTestRule.onNodeWithTag("stories-tap-left").performClick()

        assertEquals(0, activeIndex)
    }

    @Test
    fun `tapping the right zone on the last slide calls onFinish`() {
        var activeIndex by mutableIntStateOf(1)
        var finished = false

        composeTestRule.setContent {
            AlongsideTheme {
                StoriesChrome(
                    slideCount = 2,
                    activeIndex = activeIndex,
                    onActiveIndexChange = { activeIndex = it },
                    autoAdvance = false,
                    onFinish = { finished = true },
                    modifier = Modifier.fillMaxSize(),
                ) { index, _ ->
                    Text("Slide $index")
                }
            }
        }

        composeTestRule.onNodeWithTag("stories-tap-right").performClick()

        assertTrue(finished)
        assertEquals(1, activeIndex)
    }

    @Test
    fun `auto-advance moves to the next slide once its duration elapses, without waiting real time`() {
        composeTestRule.mainClock.autoAdvance = false
        var activeIndex by mutableStateOf(0)

        composeTestRule.setContent {
            AlongsideTheme {
                StoriesChrome(
                    slideCount = 3,
                    activeIndex = activeIndex,
                    onActiveIndexChange = { activeIndex = it },
                    slideDurationMillis = 200,
                    modifier = Modifier.fillMaxSize().background(Color.Black),
                ) { index, _ ->
                    Text("Slide $index")
                }
            }
        }

        // 250ms crosses one 200ms slide duration but not two - lands on index 1, not 2.
        composeTestRule.mainClock.advanceTimeBy(250L)
        composeTestRule.waitForIdle()

        assertEquals(1, activeIndex)
    }

    @Test
    fun `auto-advance on the last slide calls onFinish instead of advancing past the end`() {
        composeTestRule.mainClock.autoAdvance = false
        var activeIndex by mutableStateOf(1)
        var finished = false

        composeTestRule.setContent {
            AlongsideTheme {
                StoriesChrome(
                    slideCount = 2,
                    activeIndex = activeIndex,
                    onActiveIndexChange = { activeIndex = it },
                    slideDurationMillis = 200,
                    onFinish = { finished = true },
                    modifier = Modifier.fillMaxSize().background(Color.Black),
                ) { index, _ ->
                    Text("Slide $index")
                }
            }
        }

        composeTestRule.mainClock.advanceTimeBy(500L)
        composeTestRule.waitForIdle()

        assertEquals(1, activeIndex)
        assertTrue(finished)
    }
}
