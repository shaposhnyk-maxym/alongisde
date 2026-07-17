package com.alongside.feature.onboarding.presentation

public sealed interface OnboardingIntent {
    public data object RequestPhotoPermission : OnboardingIntent

    public data object AcknowledgeCameraGeolocation : OnboardingIntent

    public data object AcknowledgeShareSetup : OnboardingIntent

    public data object RequestNotificationPermission : OnboardingIntent

    public data object OpenAppSettings : OnboardingIntent

    /** Re-queries permission status without prompting - fired on ON_RESUME so a permission
     * granted from the system Settings screen while the user was away is picked up on return. */
    public data object RefreshPermissions : OnboardingIntent
}
