package com.alongside.feature.diary.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.alongside.core.model.diary.Photo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.time.Instant

private fun photo(id: String) =
    Photo(id = id, uri = "content://$id", takenAt = Instant.fromEpochMilliseconds(0), latitude = 0.0, longitude = 0.0)

/**
 * docs/roadmap.md M12: the stagger reveal order must follow `Episode.photos`' own order, not any
 * re-sorted/re-grouped view of it. [EpisodePhotoGallery] maps stagger index `i` straight to
 * `photos[i]`, so this pins that mapping down - [StaggerRevealColumnTest] (core:ui) already covers
 * the timing mechanics generically.
 */
@RunWith(RobolectricTestRunner::class)
class EpisodePhotoGalleryTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val photos = listOf(photo("first"), photo("second"), photo("third"))

    @Test
    fun `photos reveal in the same order they appear in the episode`() {
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            EpisodePhotoGallery(photos = photos, initiallyRevealed = false)
        }

        // Nothing revealed yet on the very first frame.
        composeTestRule.onNodeWithTag("episode-photo-first").assertDoesNotExist()
        composeTestRule.onNodeWithTag("episode-photo-second").assertDoesNotExist()
        composeTestRule.onNodeWithTag("episode-photo-third").assertDoesNotExist()

        // Just past the first stagger step: only the first photo is in.
        composeTestRule.mainClock.advanceTimeBy(90L)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("episode-photo-first").assertIsDisplayed()
        composeTestRule.onNodeWithTag("episode-photo-second").assertDoesNotExist()
        composeTestRule.onNodeWithTag("episode-photo-third").assertDoesNotExist()

        // Just past the second stagger step: first and second, still not third.
        composeTestRule.mainClock.advanceTimeBy(80L)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("episode-photo-first").assertIsDisplayed()
        composeTestRule.onNodeWithTag("episode-photo-second").assertIsDisplayed()
        composeTestRule.onNodeWithTag("episode-photo-third").assertDoesNotExist()

        // Past the full duration: all three, in order.
        composeTestRule.mainClock.advanceTimeBy(500L)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("episode-photo-first").assertIsDisplayed()
        composeTestRule.onNodeWithTag("episode-photo-second").assertIsDisplayed()
        composeTestRule.onNodeWithTag("episode-photo-third").assertIsDisplayed()
    }

    @Test
    fun `initiallyRevealed shows every photo already in order on the first frame`() {
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            EpisodePhotoGallery(photos = photos, initiallyRevealed = true)
        }

        composeTestRule.onNodeWithTag("episode-photo-first").assertIsDisplayed()
        composeTestRule.onNodeWithTag("episode-photo-second").assertIsDisplayed()
        composeTestRule.onNodeWithTag("episode-photo-third").assertIsDisplayed()
    }
}
