package com.alongside.feature.onboarding.presentation

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.alongside.core.ui.theme.AlongsideTheme
import com.alongside.feature.onboarding.FakeOnboardingCompletionCache
import com.alongside.feature.onboarding.FakePermissionController
import com.alongside.feature.onboarding.OnboardingPermission
import com.alongside.feature.onboarding.PermissionStatus
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class OnboardingPermissionRecoveryTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `permanently denied photo permission shows a settings link that calls through to the controller`() {
        val controller =
            FakePermissionController(
                initialStatuses = mapOf(OnboardingPermission.PHOTOS to PermissionStatus.DENIED_PERMANENTLY),
            )
        val container = OnboardingContainer(controller, FakeOnboardingCompletionCache())
        composeTestRule.setContent {
            AlongsideTheme {
                OnboardingScreen(container)
            }
        }

        composeTestRule.onNodeWithText("Open Settings").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000L) { controller.openAppSettingsCallCount == 1 }

        assertEquals(1, controller.openAppSettingsCallCount)
    }
}
