package com.alongside.feature.settings.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.alongside.core.domain.pairing.DefaultPairingRepository
import com.alongside.core.domain.pairing.InMemoryPairingTripDataSource
import com.alongside.core.domain.pairing.InviteCodeGenerator
import com.alongside.core.domain.trip.DefaultTripManagementRepository
import com.alongside.core.model.trip.Trip
import com.alongside.core.ui.theme.AlongsideTheme
import com.alongside.feature.settings.FakeAuthSessionCache
import com.alongside.feature.settings.InMemoryTripRepository
import com.alongside.feature.settings.fakeTrip
import com.alongside.feature.settings.testAuthSession
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.orbitmvi.orbit.compose.collectSideEffect
import org.robolectric.RobolectricTestRunner

private const val WAIT_TIMEOUT_MILLIS = 5_000L

@RunWith(RobolectricTestRunner::class)
class SettingsScreenAuthorizationTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val dataSource = InMemoryPairingTripDataSource()
    private val tripRepository = InMemoryTripRepository()

    @Test
    fun `the owner sees the Delete Trip row`() {
        val trip = fakeTrip(ownerId = "owner-1", memberId = "member-1")
        seed(trip)
        setContent(uid = "owner-1")

        waitForText("Leave Trip")
        composeTestRule.onNodeWithText("Delete Trip").assertExists()
    }

    @Test
    fun `the member does not see the Delete Trip row`() {
        val trip = fakeTrip(ownerId = "owner-1", memberId = "member-1")
        seed(trip)
        setContent(uid = "member-1")

        waitForText("Leave Trip")
        composeTestRule.onNodeWithText("Delete Trip").assertDoesNotExist()
    }

    @Test
    fun `the owner deleting the trip after confirming reaches the side effect`() {
        val trip = fakeTrip(ownerId = "owner-1", memberId = "member-1")
        seed(trip)
        setContent(uid = "owner-1")

        waitForText("Delete Trip")
        composeTestRule.onNodeWithText("Delete Trip").performClick()
        waitForText("Delete Trip?")
        composeTestRule.onNodeWithText("Delete").performClick()

        waitForStep("settings-left-or-deleted")
    }

    private fun seed(trip: Trip) {
        runBlocking {
            dataSource.save(trip)
            tripRepository.upsert(trip)
        }
    }

    private fun setContent(uid: String) {
        val container =
            SettingsContainer(
                pairingRepository = DefaultPairingRepository(dataSource, InviteCodeGenerator()),
                tripManagementRepository = DefaultTripManagementRepository(tripRepository),
                authSessionCache = FakeAuthSessionCache(testAuthSession(uid)),
            )
        composeTestRule.setContent {
            AlongsideTheme {
                var leftOrDeleted by remember { mutableStateOf(false) }
                container.collectSideEffect { effect ->
                    if (effect is SettingsSideEffect.LeftOrDeletedTrip) leftOrDeleted = true
                }
                if (leftOrDeleted) {
                    Box(
                        modifier =
                            Modifier
                                .size(10.dp)
                                .semantics { contentDescription = "settings-left-or-deleted" },
                    )
                } else {
                    SettingsScreen(container, onClose = {})
                }
            }
        }
    }

    private fun waitForText(text: String) {
        composeTestRule.waitUntil(timeoutMillis = WAIT_TIMEOUT_MILLIS) {
            composeTestRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForStep(contentDescription: String) {
        composeTestRule.waitUntil(timeoutMillis = WAIT_TIMEOUT_MILLIS) {
            composeTestRule
                .onAllNodesWithContentDescription(contentDescription)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }
}
