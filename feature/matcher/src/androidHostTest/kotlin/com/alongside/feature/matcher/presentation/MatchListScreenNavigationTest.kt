package com.alongside.feature.matcher.presentation

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.alongside.core.model.SyncStatus
import com.alongside.core.model.place.PlaceCandidate
import com.alongside.core.model.place.PlaceSwipe
import com.alongside.core.model.place.SwipeDirection
import com.alongside.core.model.trip.Trip
import com.alongside.core.ui.theme.AlongsideTheme
import kotlinx.datetime.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.time.Instant

private fun matchedPlace(
    id: String,
    latitude: Double = 49.8397,
    longitude: Double = 24.0297,
    name: String = "Rynok Square",
) = PlaceCandidate(
    id = id,
    tripId = "trip-1",
    name = name,
    latitude = latitude,
    longitude = longitude,
    note = null,
    addedByUserId = "owner-1",
    syncStatus = SyncStatus.SYNCED,
    createdAt = Instant.fromEpochMilliseconds(0),
    updatedAt = Instant.fromEpochMilliseconds(0),
)

private fun mutualLikeSwipes(candidateId: String) =
    listOf(
        PlaceSwipe(
            id = "$candidateId::owner-1",
            tripId = "trip-1",
            candidateId = candidateId,
            userId = "owner-1",
            direction = SwipeDirection.LIKE,
            swipedAt = Instant.fromEpochMilliseconds(0),
            syncStatus = SyncStatus.SYNCED,
            updatedAt = Instant.fromEpochMilliseconds(0),
        ),
        PlaceSwipe(
            id = "$candidateId::member-1",
            tripId = "trip-1",
            candidateId = candidateId,
            userId = "member-1",
            direction = SwipeDirection.LIKE,
            swipedAt = Instant.fromEpochMilliseconds(0),
            syncStatus = SyncStatus.SYNCED,
            updatedAt = Instant.fromEpochMilliseconds(0),
        ),
    )

private val TestTrip =
    Trip(
        id = "trip-1",
        ownerId = "owner-1",
        memberId = "member-1",
        inviteCode = "ABCD23",
        startDate = LocalDate(2026, 7, 18),
        endDate = LocalDate(2026, 8, 1),
        syncStatus = SyncStatus.SYNCED,
        createdAt = Instant.fromEpochMilliseconds(0),
        updatedAt = Instant.fromEpochMilliseconds(0),
    )

/** docs/roadmap.md M21.6 - tapping a matched card opens maps at that place's coordinates. */
@RunWith(RobolectricTestRunner::class)
class MatchListScreenNavigationTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `tapping a matched card invokes onOpenMaps with that place`() {
        val place = matchedPlace(id = "place-1")
        var opened: PlaceCandidate? = null
        composeTestRule.setContent {
            AlongsideTheme {
                MatchListContent(
                    state =
                        MatcherState(
                            ownUserId = "owner-1",
                            trip = TestTrip,
                            candidates = listOf(place),
                            swipes = mutualLikeSwipes(place.id),
                        ),
                    onOpenMaps = { opened = it },
                )
            }
        }

        composeTestRule.onNodeWithTag("match-row-place-1").performClick()

        assert(opened?.id == "place-1") { "onOpenMaps was not invoked with the tapped place" }
    }
}
