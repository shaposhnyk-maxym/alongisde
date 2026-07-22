package com.alongside.feature.diary.presentation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import com.alongside.core.domain.diary.DayUnlockState
import com.alongside.core.model.SyncStatus
import com.alongside.core.model.diary.Episode
import com.alongside.core.ui.theme.AlongsideTheme
import kotlinx.datetime.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.time.Instant

private val SelectedDotWidth = 18.dp
private val UnselectedDotWidth = 6.dp

// A full edge-to-edge swipeLeft()/swipeRight() covers the whole (Robolectric-default) 320dp-wide
// pager in 200ms - more than one page's width, so the fling velocity carries it two pages instead
// of one. A shorter, gentler drag keeps it to exactly one page per gesture.
private fun SemanticsNodeInteraction.swipeOnePageForward() =
    performTouchInput {
        swipe(start = Offset(centerX + 80f, centerY), end = Offset(centerX - 80f, centerY), durationMillis = 300)
    }

private fun SemanticsNodeInteraction.swipeOnePageBackward() =
    performTouchInput {
        swipe(start = Offset(centerX - 80f, centerY), end = Offset(centerX + 80f, centerY), durationMillis = 300)
    }

private fun lockedDay(dayIndex: Int) =
    DiaryTimelineItem.Day(
        DiaryDayCard(
            date = LocalDate(2026, 7, 18 + dayIndex),
            dayIndex = dayIndex,
            unlockState = DayUnlockState.LOCKED,
            waitingState = DiaryDayWaitingState.PARTNER_CAPTURING,
            ownEpisodes = emptyList(),
            partnerEpisodes = emptyList(),
            ownClosedAt = null,
        ),
    )

private fun unlockedDay(dayIndex: Int) =
    DiaryTimelineItem.Day(
        DiaryDayCard(
            date = LocalDate(2026, 7, 18 + dayIndex),
            dayIndex = dayIndex,
            unlockState = DayUnlockState.UNLOCKED,
            waitingState = null,
            ownEpisodes = emptyList(),
            partnerEpisodes = emptyList(),
            ownClosedAt = null,
        ),
    )

private fun testEpisode() =
    Episode(
        id = "episode-1",
        diaryEntryId = "entry-1",
        startTime = Instant.fromEpochMilliseconds(0),
        endTime = Instant.fromEpochMilliseconds(0),
        latitude = 49.0,
        longitude = 24.0,
        placeName = "Rynok Square",
        description = "Wandered the old town.",
        descriptionAttempts = 1,
        photos = emptyList(),
        syncStatus = SyncStatus.SYNCED,
        updatedAt = Instant.fromEpochMilliseconds(0),
    )

/** A single-day carousel (page 0 is the day itself) - button-area tests key off whichever page is centered. */
private fun captureTestItems(
    ownEpisodes: List<Episode> = emptyList(),
    ownClosedAt: Instant? = null,
) = listOf(
    DiaryTimelineItem.Day(
        DiaryDayCard(
            date = LocalDate(2026, 7, 19),
            dayIndex = 1,
            unlockState = DayUnlockState.LOCKED,
            waitingState = DiaryDayWaitingState.PARTNER_CAPTURING,
            ownEpisodes = ownEpisodes,
            partnerEpisodes = emptyList(),
            ownClosedAt = ownClosedAt,
        ),
    ),
)

/**
 * docs/roadmap.md M12: swipe navigation over the carousel, and that unlocking one day never
 * changes another. Drives [DiaryTimelineContent] directly with a fixed item list rather than a
 * real Container - the carousel's swipe/peek mechanics are pure Compose UI state, independent of
 * how `items` gets built ([DiaryTimelineContainerTest] already covers that wiring).
 */
@RunWith(RobolectricTestRunner::class)
class DiaryTimelineNavigationTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    // Day 1 (locked) and Day 2 (unlocked) sit right next to each other, adjacent to the
    // countdown - swiping one step over exercises both the "neighbor peek" and the
    // "one day's unlock state never leaks into another's" accept criteria at once.
    private val items =
        listOf(
            DiaryTimelineItem.Countdown(daysUntilReunion = 5),
            lockedDay(dayIndex = 1),
            unlockedDay(dayIndex = 2),
            lockedDay(dayIndex = 3),
        )

    // A property (not a function) so this doesn't count against detekt's TooManyFunctions -
    // purely a call-site convenience, `setContent()` reads identically either way.
    private val setContent: () -> Unit = {
        composeTestRule.setContent {
            AlongsideTheme {
                DiaryTimelineContent(items = items, modifier = Modifier.fillMaxSize())
            }
        }
    }

    @Test
    fun `the carousel opens on the countdown with day 1 peeking as a neighbor`() {
        setContent()

        composeTestRule.onNodeWithTag("pager-dot-0").assertWidthIsEqualTo(SelectedDotWidth)
        composeTestRule.onNodeWithContentDescription("timeline-countdown").assertExists()
        composeTestRule.onNodeWithTag("timeline-page-1").assertExists()
    }

    @Test
    fun `swiping forward moves to the next day, with both neighbors peeking`() {
        setContent()

        composeTestRule.onNodeWithTag("timeline-pager").swipeOnePageForward()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("pager-dot-1").assertWidthIsEqualTo(SelectedDotWidth)
        composeTestRule.onNodeWithTag("pager-dot-0").assertWidthIsEqualTo(UnselectedDotWidth)
        composeTestRule.onNodeWithTag("timeline-page-0").assertExists()
        composeTestRule.onNodeWithTag("timeline-page-1").assertExists()
        composeTestRule.onNodeWithTag("timeline-page-2").assertExists()
    }

    @Test
    fun `swiping backward returns to the previous page`() {
        setContent()

        composeTestRule.onNodeWithTag("timeline-pager").swipeOnePageForward()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("timeline-pager").swipeOnePageForward()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("pager-dot-2").assertWidthIsEqualTo(SelectedDotWidth)

        composeTestRule.onNodeWithTag("timeline-pager").swipeOnePageBackward()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("pager-dot-1").assertWidthIsEqualTo(SelectedDotWidth)
    }

    @Test
    fun `tapping Add Photos invokes the onAddPhotos callback with the selected day's date`() {
        var addPhotosDate: LocalDate? = null
        composeTestRule.setContent {
            AlongsideTheme {
                DiaryTimelineContent(
                    items = captureTestItems(),
                    modifier = Modifier.fillMaxSize(),
                    onAddPhotos = { date -> addPhotosDate = date },
                )
            }
        }

        composeTestRule.onNodeWithTag("timeline-add-photos").performClick()

        assert(addPhotosDate == LocalDate(2026, 7, 19)) { "onAddPhotos was not invoked with the expected date" }
    }

    @Test
    fun `tapping Add Photos on a day with own episodes shows the continue dialog, not direct capture`() {
        var addPhotosCalled = false
        composeTestRule.setContent {
            AlongsideTheme {
                DiaryTimelineContent(
                    items = captureTestItems(ownEpisodes = listOf(testEpisode())),
                    modifier = Modifier.fillMaxSize(),
                    onAddPhotos = { addPhotosCalled = true },
                )
            }
        }

        composeTestRule.onNodeWithTag("timeline-add-photos").performClick()

        composeTestRule.onNodeWithTag("timeline-continue-capture-dialog").assertExists()
        assert(!addPhotosCalled) { "onAddPhotos fired before the dialog was confirmed" }

        composeTestRule.onNodeWithTag("timeline-continue-capture-confirm").performClick()

        assert(addPhotosCalled) { "onAddPhotos was not invoked after confirming the dialog" }
    }

    @Test
    fun `tapping Close Day invokes the onCloseDay callback with the selected day's date`() {
        var closeDayDate: LocalDate? = null
        composeTestRule.setContent {
            AlongsideTheme {
                DiaryTimelineContent(
                    items = captureTestItems(ownEpisodes = listOf(testEpisode())),
                    modifier = Modifier.fillMaxSize(),
                    onCloseDay = { date -> closeDayDate = date },
                )
            }
        }

        composeTestRule.onNodeWithTag("timeline-close-day").performClick()

        assert(closeDayDate == LocalDate(2026, 7, 19)) { "onCloseDay was not invoked with the expected date" }
    }

    @Test
    fun `a day with no own episodes never shows Close Day, even though Add Photos is available`() {
        composeTestRule.setContent {
            AlongsideTheme {
                DiaryTimelineContent(items = captureTestItems(), modifier = Modifier.fillMaxSize())
            }
        }

        composeTestRule.onNodeWithTag("timeline-add-photos").assertExists()
        composeTestRule.onNodeWithTag("timeline-close-day").assertDoesNotExist()
    }

    @Test
    fun `a day whose date has already passed hides Add Photos and Close Day entirely`() {
        composeTestRule.setContent {
            AlongsideTheme {
                DiaryTimelineContent(
                    items = captureTestItems(ownEpisodes = listOf(testEpisode())),
                    today = LocalDate(2026, 7, 20),
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        composeTestRule.onNodeWithTag("timeline-add-photos").assertDoesNotExist()
        composeTestRule.onNodeWithTag("timeline-close-day").assertDoesNotExist()
        composeTestRule.onNodeWithTag("timeline-day-closed-label").assertDoesNotExist()
    }

    @Test
    fun `a closed entry hides both Add Photos and Close Day, showing a closed label instead`() {
        composeTestRule.setContent {
            AlongsideTheme {
                DiaryTimelineContent(
                    items =
                        captureTestItems(
                            ownEpisodes = listOf(testEpisode()),
                            ownClosedAt = Instant.fromEpochMilliseconds(1),
                        ),
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        composeTestRule.onNodeWithTag("timeline-day-closed-label").assertExists()
        composeTestRule.onNodeWithTag("timeline-add-photos").assertDoesNotExist()
        composeTestRule.onNodeWithTag("timeline-close-day").assertDoesNotExist()
    }

    @Test
    fun `unlocking one day's card never changes a neighboring locked day's card`() {
        setContent()

        composeTestRule.onNodeWithTag("timeline-pager").swipeOnePageForward()
        composeTestRule.waitForIdle()

        // Now centered on Day 1 (locked); Day 2 (unlocked) peeks as its right-hand neighbor.
        // Both are composed at once, each still showing its own independently-computed state.
        composeTestRule.onNodeWithContentDescription("timeline-day-1-locked-PARTNER_CAPTURING").assertExists()
        composeTestRule.onNodeWithContentDescription("timeline-day-2-unlocked").assertExists()
    }
}
