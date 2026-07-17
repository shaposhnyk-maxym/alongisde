package com.alongside.feature.onboarding

public enum class OnboardingPermission {
    PHOTOS,
    NOTIFICATIONS,
}

/**
 * Seam around the platform's runtime permission APIs (Android's ActivityCompat/ActivityResultContracts,
 * a future iOS equivalent). Deliberately callback-based rather than `suspend` so a Swift class can
 * implement it directly, the same reasoning as `GoogleAuthProvider` - `OnboardingContainer` adapts
 * the callback to a coroutine.
 */
public interface PermissionController {
    public fun status(permission: OnboardingPermission): PermissionStatus

    public fun request(
        permission: OnboardingPermission,
        onResult: (PermissionStatus) -> Unit,
    )

    public fun openAppSettings()
}
