package com.alongside.feature.onboarding

import platform.Foundation.NSURL
import platform.Photos.PHAccessLevelReadWrite
import platform.Photos.PHAuthorizationStatus
import platform.Photos.PHPhotoLibrary
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatus
import platform.UserNotifications.UNUserNotificationCenter

/**
 * Real iOS implementation, replacing [StubPermissionController] (deleted).
 *
 * Unlike [GoogleAuthProvider][com.alongside.feature.auth.GoogleAuthProvider] (which had to be
 * implemented in Swift because GIDSignIn is a Swift Package Manager dependency), `PHPhotoLibrary`
 * and `UNUserNotificationCenter` are Apple system frameworks with auto-generated cinterop
 * bindings reachable directly from Kotlin/Native - confirmed live (docs/roadmap.md M7's Unit 1
 * spike), no Swift intermediary needed here.
 *
 * iOS has no equivalent of Android's `shouldShowRequestPermissionRationale` - once authorization
 * leaves "not determined", the system will never show its permission dialog again for this app.
 * There is no "asked once, can still re-prompt" middle state the way Android's DENIED is. So this
 * controller never produces [PermissionStatus.DENIED] - any post-ask non-grant maps straight to
 * [PermissionStatus.DENIED_PERMANENTLY], which already renders the correct "Open Settings" UI
 * (see `PermissionActions` in OnboardingStepScreens.kt). This is an intentional platform
 * divergence, not a bug - parallel to M6's own "4 states, not 2" Android-side note.
 *
 * `PHAuthorizationStatus.PHAuthorizationStatusLimited` (iOS 14+, user granted only some photos)
 * maps to [PermissionStatus.GRANTED] - there is no 5th state in the domain model, and limited
 * access still lets photo picking work, so this is the least-surprising mapping.
 */
public class IosPermissionController : PermissionController {
    // UNUserNotificationCenter's settings query is callback-based, but `status()` must return
    // synchronously - cache the last known status, refreshed eagerly on construction and again
    // whenever request() is called, matching OnboardingContainer's own RefreshPermissions/
    // ON_RESUME re-query pattern on the Android side.
    private var cachedNotificationStatus: PermissionStatus = PermissionStatus.NOT_DETERMINED

    init {
        refreshNotificationStatus()
    }

    override fun status(permission: OnboardingPermission): PermissionStatus =
        when (permission) {
            OnboardingPermission.PHOTOS -> photoStatus()
            OnboardingPermission.NOTIFICATIONS -> cachedNotificationStatus
        }

    override fun request(
        permission: OnboardingPermission,
        onResult: (PermissionStatus) -> Unit,
    ) {
        when (permission) {
            OnboardingPermission.PHOTOS -> {
                PHPhotoLibrary.requestAuthorizationForAccessLevel(PHAccessLevelReadWrite) { status ->
                    onResult(photoAuthorizationStatusToPermissionStatus(status))
                }
            }
            OnboardingPermission.NOTIFICATIONS -> {
                val options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
                UNUserNotificationCenter.currentNotificationCenter().requestAuthorizationWithOptions(options) { _, _ ->
                    refreshNotificationStatus { onResult(cachedNotificationStatus) }
                }
            }
        }
    }

    override fun openAppSettings() {
        val url = NSURL(string = UIApplicationOpenSettingsURLString)
        UIApplication.sharedApplication.openURL(url)
    }

    private fun photoStatus(): PermissionStatus {
        val status = PHPhotoLibrary.authorizationStatusForAccessLevel(PHAccessLevelReadWrite)
        return photoAuthorizationStatusToPermissionStatus(status)
    }

    private fun refreshNotificationStatus(onDone: () -> Unit = {}) {
        UNUserNotificationCenter.currentNotificationCenter().getNotificationSettingsWithCompletionHandler { settings ->
            cachedNotificationStatus =
                settings?.authorizationStatus?.let { notificationAuthorizationStatusToPermissionStatus(it) }
                    ?: PermissionStatus.NOT_DETERMINED
            onDone()
        }
    }
}

// PHAuthorizationStatus/UNAuthorizationStatus are both plain `Long` typealiases in Kotlin/Native's
// cinterop (NS_ENUM doesn't get a distinct type) - two same-shaped extension functions on Long
// would be an unresolvable overload ambiguity, hence named top-level functions instead.
private fun photoAuthorizationStatusToPermissionStatus(status: PHAuthorizationStatus): PermissionStatus =
    when (status) {
        platform.Photos.PHAuthorizationStatusNotDetermined -> PermissionStatus.NOT_DETERMINED
        platform.Photos.PHAuthorizationStatusAuthorized, platform.Photos.PHAuthorizationStatusLimited ->
            PermissionStatus.GRANTED
        else -> PermissionStatus.DENIED_PERMANENTLY
    }

private fun notificationAuthorizationStatusToPermissionStatus(status: UNAuthorizationStatus): PermissionStatus =
    when (status) {
        platform.UserNotifications.UNAuthorizationStatusNotDetermined -> PermissionStatus.NOT_DETERMINED
        platform.UserNotifications.UNAuthorizationStatusAuthorized,
        platform.UserNotifications.UNAuthorizationStatusProvisional,
        platform.UserNotifications.UNAuthorizationStatusEphemeral,
        -> PermissionStatus.GRANTED
        else -> PermissionStatus.DENIED_PERMANENTLY
    }
