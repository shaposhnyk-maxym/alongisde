package com.alongside.feature.onboarding.presentation

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.alongside.core.ui.theme.AlongsideTheme
import com.alongside.feature.onboarding.FakePermissionController
import com.alongside.feature.onboarding.OnboardingPermission
import com.alongside.feature.onboarding.PermissionStatus
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val WAIT_TIMEOUT_MILLIS = 5_000L

@RunWith(RobolectricTestRunner::class)
class OnboardingScreenNavigationTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `nothing granted starts at photo permission and walks through all four steps to completion`() {
        val controller = FakePermissionController(requestResult = { PermissionStatus.GRANTED })
        setContent(controller)

        waitForStep("PHOTO_PERMISSION")
        composeTestRule.onNodeWithText("Allow Photo Access").performClick()

        waitForStep("CAMERA_GEOLOCATION")
        composeTestRule.onNodeWithText("Got It").performClick()

        waitForStep("SHARE_SETUP")
        composeTestRule.onNodeWithText("Continue").performClick()

        waitForStep("NOTIFICATION_PERMISSION")
        composeTestRule.onNodeWithText("Allow Notifications").performClick()

        waitForStep("COMPLETE")
    }

    @Test
    fun `photo permission already granted skips straight to the camera geolocation step`() {
        val controller =
            FakePermissionController(
                initialStatuses = mapOf(OnboardingPermission.PHOTOS to PermissionStatus.GRANTED),
            )
        setContent(controller)

        waitForStep("CAMERA_GEOLOCATION")
    }

    @Test
    fun `both permissions already granted skips both permission steps entirely`() {
        val controller =
            FakePermissionController(
                initialStatuses =
                    mapOf(
                        OnboardingPermission.PHOTOS to PermissionStatus.GRANTED,
                        OnboardingPermission.NOTIFICATIONS to PermissionStatus.GRANTED,
                    ),
            )
        setContent(controller)

        waitForStep("CAMERA_GEOLOCATION")
        composeTestRule.onNodeWithText("Got It").performClick()

        waitForStep("SHARE_SETUP")
        composeTestRule.onNodeWithText("Continue").performClick()

        waitForStep("COMPLETE")
    }

    @Test
    fun `denied photo permission still shows the photo permission step with a rationale banner`() {
        val controller =
            FakePermissionController(
                initialStatuses = mapOf(OnboardingPermission.PHOTOS to PermissionStatus.DENIED),
            )
        setContent(controller)

        waitForStep("PHOTO_PERMISSION")
        composeTestRule.onNodeWithText("Try Again").assertExists()
    }

    private fun setContent(controller: FakePermissionController) {
        val container = OnboardingContainer(controller)
        composeTestRule.setContent {
            AlongsideTheme {
                OnboardingScreen(container)
            }
        }
    }

    // The container's intent{} coroutines don't necessarily settle within a single waitForIdle()
    // call under Robolectric's UnconfinedTestDispatcher when other work is competing for the
    // dispatcher (as happens running the whole suite, not just this test in isolation) - polling
    // for the expected step to actually appear is robust to that scheduling variance.
    private fun waitForStep(step: String) {
        val contentDescription = if (step == "COMPLETE") "onboarding-complete" else "onboarding-step-$step"
        composeTestRule.waitUntil(timeoutMillis = WAIT_TIMEOUT_MILLIS) {
            composeTestRule
                .onAllNodesWithContentDescription(contentDescription)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }
}
