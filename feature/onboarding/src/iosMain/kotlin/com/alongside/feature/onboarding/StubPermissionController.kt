package com.alongside.feature.onboarding

/**
 * Temporary placeholder until M7's real iOS permissions work lands (Photos/PHPickerViewController,
 * UNUserNotificationCenter) - always reports [PermissionStatus.NOT_DETERMINED] and never actually
 * prompts. Exists purely so `AlongsideApp()` has a concrete [PermissionController] to compose
 * against on iOS now that `iosApp` exists.
 */
public class StubPermissionController : PermissionController {
    override fun status(permission: OnboardingPermission): PermissionStatus = PermissionStatus.NOT_DETERMINED

    override fun request(
        permission: OnboardingPermission,
        onResult: (PermissionStatus) -> Unit,
    ) {
        onResult(PermissionStatus.NOT_DETERMINED)
    }

    override fun openAppSettings() {
        // No-op - see class kdoc.
    }
}
