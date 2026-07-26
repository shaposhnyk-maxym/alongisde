package com.alongside.feature.diary.presentation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.alongside.core.model.SyncStatus
import com.alongside.core.model.pretrip.PreTripPhoto
import com.alongside.core.ui.theme.AlongsideTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.time.Instant

private fun testPreTripPhoto(id: String) =
    PreTripPhoto(
        id = id,
        tripId = "trip-1",
        userId = "owner-1",
        uri = "content://pretrip/$id",
        takenAt = Instant.fromEpochMilliseconds(0),
        latitude = 49.0,
        longitude = 24.0,
        syncStatus = SyncStatus.SYNCED,
    )

/**
 * The Countdown card's capture UI (docs/roadmap.md M19.8) - split out from
 * [DiaryTimelineNavigationTest] purely to keep that class under detekt's `TooManyFunctions`
 * threshold, not because these tests are conceptually separate from the rest of the carousel's
 * button-area behavior.
 */
@RunWith(RobolectricTestRunner::class)
class CountdownCaptureTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `tapping Add Photos on the Countdown card invokes onAddPreTripPhotos, not onAddPhotos`() {
        var preTripPhotosCalled = false
        var addPhotosCalled = false
        composeTestRule.setContent {
            AlongsideTheme {
                DiaryTimelineContent(
                    items = listOf(DiaryTimelineItem.Countdown(5, emptyList(), emptyList())),
                    modifier = Modifier.fillMaxSize(),
                    onAddPhotos = { addPhotosCalled = true },
                    onAddPreTripPhotos = { preTripPhotosCalled = true },
                )
            }
        }

        composeTestRule.onNodeWithTag("timeline-add-photos").performClick()

        assert(preTripPhotosCalled) { "onAddPreTripPhotos was not invoked" }
        assert(!addPhotosCalled) { "onAddPhotos should never fire for the Countdown card" }
    }

    @Test
    fun `the countdown card renders a gallery tile per pre-trip photo`() {
        composeTestRule.setContent {
            AlongsideTheme {
                DiaryTimelineContent(
                    items =
                        listOf(
                            DiaryTimelineItem.Countdown(
                                daysUntilReunion = 5,
                                ownPhotos = listOf(testPreTripPhoto("own-1"), testPreTripPhoto("own-2")),
                                partnerPhotos = listOf(testPreTripPhoto("partner-1")),
                            ),
                        ),
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        composeTestRule.onNodeWithTag("pretrip-photo-own-1").assertExists()
        composeTestRule.onNodeWithTag("pretrip-photo-own-2").assertExists()
        composeTestRule.onNodeWithTag("pretrip-photo-partner-1").assertExists()
    }

    @Test
    fun `the countdown card as the selected page shows both Add Photos and Close Day`() {
        composeTestRule.setContent {
            AlongsideTheme {
                DiaryTimelineContent(
                    items = listOf(DiaryTimelineItem.Countdown(5, emptyList(), emptyList())),
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        composeTestRule.onNodeWithTag("timeline-add-photos").assertExists()
        composeTestRule.onNodeWithTag("timeline-close-day").assertExists()
    }

    @Test
    fun `tapping Close Day on the Countdown card invokes onFlushPreTripPhotoSync`() {
        var flushCalled = false
        composeTestRule.setContent {
            AlongsideTheme {
                DiaryTimelineContent(
                    items = listOf(DiaryTimelineItem.Countdown(5, emptyList(), emptyList())),
                    modifier = Modifier.fillMaxSize(),
                    onFlushPreTripPhotoSync = { flushCalled = true },
                )
            }
        }

        composeTestRule.onNodeWithTag("timeline-close-day").performClick()

        assert(flushCalled) { "onFlushPreTripPhotoSync was not invoked" }
    }
}
