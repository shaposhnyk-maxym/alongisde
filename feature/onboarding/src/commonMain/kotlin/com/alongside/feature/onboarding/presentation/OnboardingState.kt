package com.alongside.feature.onboarding.presentation

import androidx.compose.runtime.Immutable
import com.alongside.feature.onboarding.OnboardingStep
import com.alongside.feature.onboarding.PermissionStatus
import com.alongside.feature.onboarding.nextOnboardingStep

@Immutable
public data class OnboardingState(
    val photoPermission: PermissionStatus = PermissionStatus.NOT_DETERMINED,
    val cameraGeolocationAcknowledged: Boolean = false,
    val shareSetupAcknowledged: Boolean = false,
    val notificationPermission: PermissionStatus = PermissionStatus.NOT_DETERMINED,
) {
    /** Derived, never stored directly, so it can never drift out of sync with the flags above. */
    val currentStep: OnboardingStep?
        get() =
            nextOnboardingStep(
                photoPermission = photoPermission,
                cameraGeolocationAcknowledged = cameraGeolocationAcknowledged,
                shareSetupAcknowledged = shareSetupAcknowledged,
                notificationPermission = notificationPermission,
            )
}
