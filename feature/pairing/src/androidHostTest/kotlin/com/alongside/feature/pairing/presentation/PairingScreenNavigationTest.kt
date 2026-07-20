package com.alongside.feature.pairing.presentation

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
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import com.alongside.core.domain.pairing.DefaultPairingRepository
import com.alongside.core.domain.pairing.InMemoryPairingTripDataSource
import com.alongside.core.domain.pairing.InviteCodeGenerator
import com.alongside.core.ui.theme.AlongsideTheme
import com.alongside.feature.pairing.FakeAuthSessionCache
import com.alongside.feature.pairing.fakeTrip
import com.alongside.feature.pairing.testAuthSession
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.orbitmvi.orbit.compose.collectSideEffect
import org.robolectric.RobolectricTestRunner

private const val WAIT_TIMEOUT_MILLIS = 5_000L

@RunWith(RobolectricTestRunner::class)
class PairingScreenNavigationTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val dataSource = InMemoryPairingTripDataSource()

    @Test
    fun `create shows the invite code with a waiting note until the partner joins`() {
        setContent()

        waitForStep("CHOICE")
        composeTestRule.onNodeWithText("Create a Trip").performClick()

        waitForStep("CREATE_PICK_DATES")
        // The picker opens pre-filled with a default range - confirming it as-is is enough to
        // exercise the step transition; the actual date-selection logic is covered by
        // PairingContainerTest against TripDatesChanged directly.
        composeTestRule.onNodeWithText("Confirm Dates").performClick()

        waitForStep("CREATE_SHOW_CODE")
        composeTestRule.onNodeWithText("Waiting for your partner", substring = true).assertExists()

        val created = runBlocking { dataSource.snapshot().single() }
        runBlocking { dataSource.save(created.copy(memberId = "partner-uid")) }

        waitForStep("PAIRED")
    }

    @Test
    fun `back from the date step returns to choice`() {
        setContent()

        waitForStep("CHOICE")
        composeTestRule.onNodeWithText("Create a Trip").performClick()

        waitForStep("CREATE_PICK_DATES")
        composeTestRule.onNodeWithText("Back").performClick()

        waitForStep("CHOICE")
    }

    @Test
    fun `join with a valid code succeeds`() {
        runBlocking { dataSource.save(fakeTrip(ownerId = "partner-uid", inviteCode = "ABCD23")) }
        setContent()

        waitForStep("CHOICE")
        composeTestRule.onNodeWithText("Join with a Code").performClick()

        waitForStep("JOIN_ENTER_CODE")
        composeTestRule.onNodeWithTag("pairing-code-input").performTextInput("ABCD23")
        composeTestRule.onNodeWithText("Join Trip").performClick()

        waitForStep("PAIRED")
    }

    @Test
    fun `join with an unknown code shows the invalid-code error and stays on the join step`() {
        setContent()

        waitForStep("CHOICE")
        composeTestRule.onNodeWithText("Join with a Code").performClick()

        waitForStep("JOIN_ENTER_CODE")
        composeTestRule.onNodeWithTag("pairing-code-input").performTextInput("ZZZZ99")
        composeTestRule.onNodeWithText("Join Trip").performClick()

        waitForError("INVALID_CODE")
        waitForStep("JOIN_ENTER_CODE")
    }

    @Test
    fun `join with an already-used code shows the already-used error`() {
        runBlocking {
            dataSource.save(
                fakeTrip(ownerId = "partner-uid", memberId = "third-uid", inviteCode = "USED22"),
            )
        }
        setContent()

        waitForStep("CHOICE")
        composeTestRule.onNodeWithText("Join with a Code").performClick()

        waitForStep("JOIN_ENTER_CODE")
        composeTestRule.onNodeWithTag("pairing-code-input").performTextInput("USED22")
        composeTestRule.onNodeWithText("Join Trip").performClick()

        waitForError("ALREADY_USED")
    }

    @Test
    fun `back from the join step returns to the choice`() {
        setContent()

        waitForStep("CHOICE")
        composeTestRule.onNodeWithText("Join with a Code").performClick()

        waitForStep("JOIN_ENTER_CODE")
        composeTestRule.onNodeWithText("Back").performClick()

        waitForStep("CHOICE")
    }

    private fun setContent() {
        val container =
            PairingContainer(
                pairingRepository = DefaultPairingRepository(dataSource, InviteCodeGenerator()),
                authSessionCache = FakeAuthSessionCache(testAuthSession("uid-1")),
            )
        composeTestRule.setContent {
            AlongsideTheme {
                var paired by remember { mutableStateOf(false) }
                container.collectSideEffect { effect ->
                    if (effect is PairingSideEffect.Paired) {
                        paired = true
                    }
                }
                if (paired) {
                    Box(
                        modifier =
                            Modifier
                                .size(10.dp)
                                .semantics { contentDescription = "pairing-paired" },
                    )
                } else {
                    PairingScreen(container)
                }
            }
        }
    }

    // Same polling rationale as OnboardingScreenNavigationTest: the container's coroutines don't
    // necessarily settle within a single waitForIdle() under Robolectric's test dispatcher.
    private fun waitForStep(step: String) {
        val contentDescription = if (step == "PAIRED") "pairing-paired" else "pairing-step-$step"
        composeTestRule.waitUntil(timeoutMillis = WAIT_TIMEOUT_MILLIS) {
            composeTestRule
                .onAllNodesWithContentDescription(contentDescription)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    private fun waitForError(error: String) {
        composeTestRule.waitUntil(timeoutMillis = WAIT_TIMEOUT_MILLIS) {
            composeTestRule
                .onAllNodesWithContentDescription("pairing-join-error-$error")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }
}
