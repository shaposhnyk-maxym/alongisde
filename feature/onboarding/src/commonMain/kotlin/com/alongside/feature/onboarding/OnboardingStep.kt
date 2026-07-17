package com.alongside.feature.onboarding

public enum class OnboardingStep {
    PHOTO_PERMISSION,
    CAMERA_GEOLOCATION,
    SHARE_SETUP,
    NOTIFICATION_PERMISSION,
}

public enum class PermissionStatus {
    GRANTED,
    NOT_DETERMINED,
    DENIED,
    DENIED_PERMANENTLY,
}

/** Pure step sequencing rule: the two acknowledgement steps always show, the two permission steps
 * are skipped only once actually [PermissionStatus.GRANTED] - [PermissionStatus.DENIED] and
 * [PermissionStatus.DENIED_PERMANENTLY] both still block progression past their step. */
public fun nextOnboardingStep(
    photoPermission: PermissionStatus,
    cameraGeolocationAcknowledged: Boolean,
    shareSetupAcknowledged: Boolean,
    notificationPermission: PermissionStatus,
): OnboardingStep? =
    when {
        photoPermission != PermissionStatus.GRANTED -> OnboardingStep.PHOTO_PERMISSION
        !cameraGeolocationAcknowledged -> OnboardingStep.CAMERA_GEOLOCATION
        !shareSetupAcknowledged -> OnboardingStep.SHARE_SETUP
        notificationPermission != PermissionStatus.GRANTED -> OnboardingStep.NOTIFICATION_PERMISSION
        else -> null
    }
