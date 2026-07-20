package com.alongside.feature.diary.presentation

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.alongside.core.domain.diary.DayUnlockState
import com.alongside.core.model.SyncStatus
import com.alongside.core.model.diary.Episode
import com.alongside.core.model.diary.Photo
import com.alongside.core.ui.theme.AlongsideTheme
import kotlinx.datetime.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.time.Instant

/**
 * Reproduces the "typewriter keeps restarting" report from manual testing: the Timeline's
 * periodic trip-content poll (every 5s, [DiaryTimelineDataSource]) re-emits a fresh
 * `DiaryDayCard`/`Episode` on every tick even when nothing actually changed - value-equal, but a
 * new object instance every time, since `Episode`/`Photo` (core:model, no Compose dependency,
 * unmarked `@Immutable`/`@Stable`) can make Compose treat the whole card subtree as unskippable.
 */
@RunWith(RobolectricTestRunner::class)
class DiaryTimelineCardsRecompositionTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val description = "We wandered into the old quarter and lost track of time."

    // Deliberately ignores its own parameter in the returned value - only the OBJECT INSTANCE
    // differs between calls, every field is identical, isolating "same value, new instance" from
    // "value actually changed".
    private fun dayCard(recomposeToken: Int): DiaryDayCard {
        recomposeToken.let { /* read, to make this call depend on the trigger */ }
        return DiaryDayCard(
            date = LocalDate(2026, 7, 19),
            dayIndex = 1,
            unlockState = DayUnlockState.UNLOCKED,
            waitingState = null,
            ownEpisodes = listOf(episode()),
            partnerEpisodes = emptyList(),
            ownClosedAt = null,
        )
    }

    private fun episode() =
        Episode(
            id = "episode-1",
            diaryEntryId = "entry-1",
            startTime = Instant.fromEpochMilliseconds(0),
            endTime = Instant.fromEpochMilliseconds(0),
            latitude = 49.0,
            longitude = 24.0,
            placeName = "Rynok Square",
            description = description,
            descriptionAttempts = 1,
            photos =
                listOf(
                    Photo(
                        id = "photo-1",
                        uri = "content://photo-1",
                        takenAt = Instant.fromEpochMilliseconds(0),
                        latitude = 49.0,
                        longitude = 24.0,
                    ),
                ),
            syncStatus = SyncStatus.SYNCED,
            updatedAt = Instant.fromEpochMilliseconds(0),
        )

    @Test
    fun `typewriter stays fully revealed across a poll-driven recomposition with value-equal data`() {
        composeTestRule.mainClock.autoAdvance = false
        var recomposeTrigger by mutableIntStateOf(0)

        composeTestRule.setContent {
            AlongsideTheme {
                DiaryTimelineItemCard(
                    item = DiaryTimelineItem.Day(dayCard(recomposeTrigger)),
                    modifier = Modifier.size(360.dp, 640.dp),
                )
            }
        }

        composeTestRule.mainClock.advanceTimeBy(description.length * 40L + 500L)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(description).assertExists()

        // Simulate the periodic poll: a brand new (but value-equal) DiaryDayCard/Episode instance.
        recomposeTrigger++
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(100L)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(description).assertExists()
    }

    @Test
    fun `typewriter stays revealed inside the real HorizontalPager across a poll-driven items update`() {
        composeTestRule.mainClock.autoAdvance = false
        var recomposeTrigger by mutableIntStateOf(0)

        composeTestRule.setContent {
            AlongsideTheme {
                DiaryTimelineContent(
                    items = listOf(DiaryTimelineItem.Day(dayCard(recomposeTrigger))),
                )
            }
        }

        composeTestRule.mainClock.advanceTimeBy(description.length * 40L + 500L)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(description).assertExists()

        // Simulate several poll ticks in a row, same as the real 5s trip-content poll would.
        repeat(5) {
            recomposeTrigger++
            composeTestRule.waitForIdle()
            composeTestRule.mainClock.advanceTimeBy(100L)
            composeTestRule.waitForIdle()
        }

        composeTestRule.onNodeWithText(description).assertExists()
    }
}
