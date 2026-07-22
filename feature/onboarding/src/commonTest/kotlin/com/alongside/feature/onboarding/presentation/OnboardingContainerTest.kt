package com.alongside.feature.onboarding.presentation

import com.alongside.feature.onboarding.FakeOnboardingCompletionCache
import com.alongside.feature.onboarding.FakePermissionController
import com.alongside.feature.onboarding.OnboardingPermission
import com.alongside.feature.onboarding.OnboardingStep
import com.alongside.feature.onboarding.PermissionStatus
import com.alongside.feature.onboarding.nextOnboardingStep
import kotlinx.coroutines.test.runTest
import org.orbitmvi.orbit.test.test
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class OnboardingContainerTest {
    @Test
    fun `onCreate seeds photo and notification permission status from the controller`() =
        runTest {
            val controller =
                FakePermissionController(
                    initialStatuses =
                        mapOf(
                            OnboardingPermission.PHOTOS to PermissionStatus.DENIED,
                            OnboardingPermission.NOTIFICATIONS to PermissionStatus.GRANTED,
                        ),
                )
            val container = OnboardingContainer(controller, FakeOnboardingCompletionCache())

            container.test(this) {
                runOnCreate()
                expectState {
                    copy(photoPermission = PermissionStatus.DENIED, notificationPermission = PermissionStatus.GRANTED)
                }
            }
        }

    @Test
    fun `requesting photo permission that succeeds updates state and does not complete yet`() =
        runTest {
            val controller = FakePermissionController(requestResult = { PermissionStatus.GRANTED })
            val container = OnboardingContainer(controller, FakeOnboardingCompletionCache())

            container.test(this) {
                runOnCreate()
                containerHost.onIntent(OnboardingIntent.RequestPhotoPermission)
                expectState { copy(photoPermission = PermissionStatus.GRANTED) }
            }
        }

    @Test
    fun `requesting photo permission that the user denies keeps state at denied without advancing`() =
        runTest {
            val controller = FakePermissionController(requestResult = { PermissionStatus.DENIED })
            val container = OnboardingContainer(controller, FakeOnboardingCompletionCache())

            container.test(this) {
                runOnCreate()
                containerHost.onIntent(OnboardingIntent.RequestPhotoPermission)
                expectState { copy(photoPermission = PermissionStatus.DENIED) }
            }
            assertEqualsStep(OnboardingStep.PHOTO_PERMISSION, controller)
        }

    @Test
    fun `acknowledging camera geolocation advances state`() =
        runTest {
            val controller =
                FakePermissionController(initialStatuses = mapOf(OnboardingPermission.PHOTOS to PermissionStatus.GRANTED))
            val container = OnboardingContainer(controller, FakeOnboardingCompletionCache())

            container.test(this) {
                runOnCreate()
                expectState { copy(photoPermission = PermissionStatus.GRANTED) }
                containerHost.onIntent(OnboardingIntent.AcknowledgeCameraGeolocation)
                expectState { copy(cameraGeolocationAcknowledged = true) }
            }
        }

    @Test
    fun `acknowledging share setup advances state`() =
        runTest {
            val controller =
                FakePermissionController(
                    initialStatuses = mapOf(OnboardingPermission.PHOTOS to PermissionStatus.GRANTED),
                )
            val container = OnboardingContainer(controller, FakeOnboardingCompletionCache())

            container.test(this) {
                runOnCreate()
                expectState { copy(photoPermission = PermissionStatus.GRANTED) }
                containerHost.onIntent(OnboardingIntent.AcknowledgeCameraGeolocation)
                expectState { copy(cameraGeolocationAcknowledged = true) }
                containerHost.onIntent(OnboardingIntent.AcknowledgeShareSetup)
                expectState { copy(shareSetupAcknowledged = true) }
            }
        }

    @Test
    fun `requesting notification permission mirrors photo permission's granted and denied handling`() =
        runTest {
            val controller =
                FakePermissionController(
                    initialStatuses =
                        mapOf(
                            OnboardingPermission.PHOTOS to PermissionStatus.GRANTED,
                        ),
                    requestResult = { PermissionStatus.DENIED },
                )
            val container = OnboardingContainer(controller, FakeOnboardingCompletionCache())

            container.test(this) {
                runOnCreate()
                expectState { copy(photoPermission = PermissionStatus.GRANTED) }
                containerHost.onIntent(OnboardingIntent.AcknowledgeCameraGeolocation)
                expectState { copy(cameraGeolocationAcknowledged = true) }
                containerHost.onIntent(OnboardingIntent.AcknowledgeShareSetup)
                expectState { copy(shareSetupAcknowledged = true) }
                containerHost.onIntent(OnboardingIntent.RequestNotificationPermission)
                expectState { copy(notificationPermission = PermissionStatus.DENIED) }
            }
        }

    @Test
    fun `completing the last remaining step posts Completed exactly once and persists it`() =
        runTest {
            val controller =
                FakePermissionController(
                    initialStatuses =
                        mapOf(
                            OnboardingPermission.PHOTOS to PermissionStatus.GRANTED,
                        ),
                    requestResult = { PermissionStatus.GRANTED },
                )
            val completionCache = FakeOnboardingCompletionCache()
            val container = OnboardingContainer(controller, completionCache)

            container.test(this) {
                runOnCreate()
                expectState { copy(photoPermission = PermissionStatus.GRANTED) }
                containerHost.onIntent(OnboardingIntent.AcknowledgeCameraGeolocation)
                expectState { copy(cameraGeolocationAcknowledged = true) }
                containerHost.onIntent(OnboardingIntent.AcknowledgeShareSetup)
                expectState { copy(shareSetupAcknowledged = true) }
                containerHost.onIntent(OnboardingIntent.RequestNotificationPermission)
                expectState { copy(notificationPermission = PermissionStatus.GRANTED) }
                expectSideEffect(OnboardingSideEffect.Completed)
            }
            assertEquals(1, completionCache.markCompletedCallCount)
            assertEquals(true, completionCache.isCompleted())
        }

    @Test
    fun `acknowledging only some steps does not mark onboarding completed in the cache`() =
        runTest {
            val controller =
                FakePermissionController(
                    initialStatuses = mapOf(OnboardingPermission.PHOTOS to PermissionStatus.GRANTED),
                )
            val completionCache = FakeOnboardingCompletionCache()
            val container = OnboardingContainer(controller, completionCache)

            container.test(this) {
                runOnCreate()
                expectState { copy(photoPermission = PermissionStatus.GRANTED) }
                containerHost.onIntent(OnboardingIntent.AcknowledgeCameraGeolocation)
                expectState { copy(cameraGeolocationAcknowledged = true) }
            }
            assertEquals(0, completionCache.markCompletedCallCount)
            assertFalse(completionCache.isCompleted())
        }

    @Test
    fun `opening app settings delegates to the controller without changing state`() =
        runTest {
            val controller = FakePermissionController()
            val container = OnboardingContainer(controller, FakeOnboardingCompletionCache())

            container.test(this) {
                runOnCreate()
                containerHost.onIntent(OnboardingIntent.OpenAppSettings)
            }
            assertEquals(1, controller.openAppSettingsCallCount)
        }

    @Test
    fun `refreshing permissions after returning from settings picks up a since-granted permission`() =
        runTest {
            val controller =
                FakePermissionController(
                    initialStatuses =
                        mapOf(
                            OnboardingPermission.PHOTOS to PermissionStatus.GRANTED,
                            OnboardingPermission.NOTIFICATIONS to PermissionStatus.DENIED_PERMANENTLY,
                        ),
                )
            val container = OnboardingContainer(controller, FakeOnboardingCompletionCache())

            container.test(this) {
                runOnCreate()
                expectState {
                    copy(photoPermission = PermissionStatus.GRANTED, notificationPermission = PermissionStatus.DENIED_PERMANENTLY)
                }
                containerHost.onIntent(OnboardingIntent.AcknowledgeCameraGeolocation)
                expectState { copy(cameraGeolocationAcknowledged = true) }
                containerHost.onIntent(OnboardingIntent.AcknowledgeShareSetup)
                expectState { copy(shareSetupAcknowledged = true) }

                // User left the app, granted notifications in system Settings, and came back.
                controller.updateStatus(OnboardingPermission.NOTIFICATIONS, PermissionStatus.GRANTED)
                containerHost.onIntent(OnboardingIntent.RefreshPermissions)
                expectState { copy(notificationPermission = PermissionStatus.GRANTED) }
                expectSideEffect(OnboardingSideEffect.Completed)
            }
        }

    private fun assertEqualsStep(
        expected: OnboardingStep,
        controller: FakePermissionController,
    ) {
        val actual =
            nextOnboardingStep(
                photoPermission = controller.status(OnboardingPermission.PHOTOS),
                cameraGeolocationAcknowledged = false,
                shareSetupAcknowledged = false,
                notificationPermission = controller.status(OnboardingPermission.NOTIFICATIONS),
            )
        assertEquals(expected, actual)
    }
}
