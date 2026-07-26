package com.alongside.feature.recap.presentation

import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.alongside.core.model.SyncStatus
import com.alongside.core.model.diary.Episode
import com.alongside.core.model.place.PlaceCandidate
import com.alongside.core.model.pretrip.PreTripPhoto
import com.alongside.core.model.recap.RecapSlide
import com.alongside.core.ui.theme.AlongsideTheme
import kotlinx.datetime.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.time.Instant

/**
 * Confirms [RecapSlideContent]'s exhaustive `when` maps each [RecapSlide] variant to its own,
 * distinct Composable (docs/roadmap.md M20.3 accept) - each of the 8 M20.3.5 Composables renders
 * one static headline string, used here as the mapping oracle without needing to touch those
 * already-`done` files.
 */
@RunWith(RobolectricTestRunner::class)
class RecapSlideContentTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `Intro maps to IntroSlideContent`() {
        composeTestRule.renderSlide(RecapSlide.Intro(cities = emptyList()))
        composeTestRule.onNodeWithText("Your trip,\ntogether").assertExists()
    }

    @Test
    fun `ParallelLives maps to ParallelLivesSlideContent`() {
        composeTestRule.renderSlide(
            RecapSlide.ParallelLives(
                ownPhoto = fixturePreTripPhoto("own"),
                partnerPhoto = fixturePreTripPhoto("partner"),
                distanceMeters = 100.0,
            ),
        )
        composeTestRule.onNodeWithText("Parallel lives").assertExists()
    }

    @Test
    fun `DayHighlight maps to DayHighlightSlideContent`() {
        composeTestRule.renderSlide(
            RecapSlide.DayHighlight(
                date = LocalDate(2026, 7, 20),
                dayIndex = 3,
                ownEpisodes = emptyList(),
                partnerEpisodes = emptyList(),
            ),
        )
        composeTestRule.onNodeWithText("Day 3").assertExists()
    }

    @Test
    fun `ClosestMoment maps to ClosestMomentSlideContent`() {
        composeTestRule.renderSlide(
            RecapSlide.ClosestMoment(
                date = LocalDate(2026, 7, 20),
                ownEpisode = fixtureEpisode("own"),
                partnerEpisode = fixtureEpisode("partner"),
                distanceMeters = 10.0,
            ),
        )
        composeTestRule.onNodeWithText("Closest moment").assertExists()
    }

    @Test
    fun `SwipeArchetype maps to SwipeArchetypeSlideContent`() {
        composeTestRule.renderSlide(RecapSlide.SwipeArchetype(ownTally = emptyList(), partnerTally = emptyList()))
        composeTestRule.onNodeWithText("Your taste").assertExists()
    }

    @Test
    fun `UnresolvedQuestion maps to UnresolvedQuestionSlideContent`() {
        composeTestRule.renderSlide(RecapSlide.UnresolvedQuestion(candidates = listOf(fixturePlaceCandidate("p1"))))
        composeTestRule.onNodeWithText("Still deciding").assertExists()
    }

    @Test
    fun `MatchList maps to MatchListSlideContent`() {
        composeTestRule.renderSlide(RecapSlide.MatchList(candidates = listOf(fixturePlaceCandidate("p1"))))
        composeTestRule.onNodeWithText("Your matches").assertExists()
    }

    @Test
    fun `Final maps to FinalSlideContent`() {
        composeTestRule.renderSlide(RecapSlide.Final(daysTogether = 7))
        composeTestRule.onNodeWithText("Days together.").assertExists()
    }
}

private fun ComposeContentTestRule.renderSlide(slide: RecapSlide) {
    setContent {
        AlongsideTheme {
            RecapSlideContent(slide)
        }
    }
}

private fun fixturePreTripPhoto(id: String) =
    PreTripPhoto(
        id = id,
        tripId = "trip-1",
        userId = "uid-1",
        uri = "content://$id",
        takenAt = Instant.fromEpochMilliseconds(0),
        latitude = 0.0,
        longitude = 0.0,
        syncStatus = SyncStatus.SYNCED,
    )

private fun fixtureEpisode(id: String) =
    Episode(
        id = id,
        diaryEntryId = "entry-1",
        startTime = Instant.fromEpochMilliseconds(0),
        endTime = Instant.fromEpochMilliseconds(0),
        latitude = 0.0,
        longitude = 0.0,
        placeName = null,
        description = null,
        descriptionAttempts = 0,
        photos = emptyList(),
        syncStatus = SyncStatus.SYNCED,
        updatedAt = Instant.fromEpochMilliseconds(0),
    )

private fun fixturePlaceCandidate(id: String) =
    PlaceCandidate(
        id = id,
        tripId = "trip-1",
        name = "Place $id",
        latitude = 0.0,
        longitude = 0.0,
        note = null,
        addedByUserId = "uid-1",
        syncStatus = SyncStatus.SYNCED,
        createdAt = Instant.fromEpochMilliseconds(0),
        updatedAt = Instant.fromEpochMilliseconds(0),
    )
