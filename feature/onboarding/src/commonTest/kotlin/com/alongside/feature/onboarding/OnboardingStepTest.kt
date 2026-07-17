package com.alongside.feature.onboarding

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OnboardingStepTest {
    @Test
    fun `nothing done returns photo permission`() {
        assertEquals(
            OnboardingStep.PHOTO_PERMISSION,
            nextOnboardingStep(
                photoPermission = PermissionStatus.NOT_DETERMINED,
                cameraGeolocationAcknowledged = false,
                shareSetupAcknowledged = false,
                notificationPermission = PermissionStatus.NOT_DETERMINED,
            ),
        )
    }

    @Test
    fun `photo permission denied still returns photo permission`() {
        assertEquals(
            OnboardingStep.PHOTO_PERMISSION,
            nextOnboardingStep(
                photoPermission = PermissionStatus.DENIED,
                cameraGeolocationAcknowledged = false,
                shareSetupAcknowledged = false,
                notificationPermission = PermissionStatus.NOT_DETERMINED,
            ),
        )
    }

    @Test
    fun `photo permission permanently denied still returns photo permission`() {
        assertEquals(
            OnboardingStep.PHOTO_PERMISSION,
            nextOnboardingStep(
                photoPermission = PermissionStatus.DENIED_PERMANENTLY,
                cameraGeolocationAcknowledged = false,
                shareSetupAcknowledged = false,
                notificationPermission = PermissionStatus.NOT_DETERMINED,
            ),
        )
    }

    @Test
    fun `photo permission already granted from the start skips straight to camera geolocation`() {
        assertEquals(
            OnboardingStep.CAMERA_GEOLOCATION,
            nextOnboardingStep(
                photoPermission = PermissionStatus.GRANTED,
                cameraGeolocationAcknowledged = false,
                shareSetupAcknowledged = false,
                notificationPermission = PermissionStatus.NOT_DETERMINED,
            ),
        )
    }

    @Test
    fun `photo granted and camera geolocation acknowledged returns share setup`() {
        assertEquals(
            OnboardingStep.SHARE_SETUP,
            nextOnboardingStep(
                photoPermission = PermissionStatus.GRANTED,
                cameraGeolocationAcknowledged = true,
                shareSetupAcknowledged = false,
                notificationPermission = PermissionStatus.NOT_DETERMINED,
            ),
        )
    }

    @Test
    fun `photo granted and both acknowledgements done returns notification permission`() {
        assertEquals(
            OnboardingStep.NOTIFICATION_PERMISSION,
            nextOnboardingStep(
                photoPermission = PermissionStatus.GRANTED,
                cameraGeolocationAcknowledged = true,
                shareSetupAcknowledged = true,
                notificationPermission = PermissionStatus.NOT_DETERMINED,
            ),
        )
    }

    @Test
    fun `notification permission denied still returns notification permission`() {
        assertEquals(
            OnboardingStep.NOTIFICATION_PERMISSION,
            nextOnboardingStep(
                photoPermission = PermissionStatus.GRANTED,
                cameraGeolocationAcknowledged = true,
                shareSetupAcknowledged = true,
                notificationPermission = PermissionStatus.DENIED,
            ),
        )
    }

    @Test
    fun `notification permission permanently denied still returns notification permission`() {
        assertEquals(
            OnboardingStep.NOTIFICATION_PERMISSION,
            nextOnboardingStep(
                photoPermission = PermissionStatus.GRANTED,
                cameraGeolocationAcknowledged = true,
                shareSetupAcknowledged = true,
                notificationPermission = PermissionStatus.DENIED_PERMANENTLY,
            ),
        )
    }

    @Test
    fun `everything done returns null`() {
        assertNull(
            nextOnboardingStep(
                photoPermission = PermissionStatus.GRANTED,
                cameraGeolocationAcknowledged = true,
                shareSetupAcknowledged = true,
                notificationPermission = PermissionStatus.GRANTED,
            ),
        )
    }

    @Test
    fun `both permissions already granted from the start still requires both acknowledgement steps`() {
        assertEquals(
            OnboardingStep.CAMERA_GEOLOCATION,
            nextOnboardingStep(
                photoPermission = PermissionStatus.GRANTED,
                cameraGeolocationAcknowledged = false,
                shareSetupAcknowledged = false,
                notificationPermission = PermissionStatus.GRANTED,
            ),
        )
    }

    @Test
    fun `both permissions granted and camera acknowledged still requires share setup`() {
        assertEquals(
            OnboardingStep.SHARE_SETUP,
            nextOnboardingStep(
                photoPermission = PermissionStatus.GRANTED,
                cameraGeolocationAcknowledged = true,
                shareSetupAcknowledged = false,
                notificationPermission = PermissionStatus.GRANTED,
            ),
        )
    }
}
