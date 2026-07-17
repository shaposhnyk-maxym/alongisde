package com.alongside.feature.onboarding.presentation

import androidx.lifecycle.ViewModel
import com.alongside.feature.onboarding.OnboardingPermission
import com.alongside.feature.onboarding.PermissionController
import com.alongside.feature.onboarding.PermissionStatus
import kotlinx.coroutines.suspendCancellableCoroutine
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.container
import kotlin.coroutines.resume

public class OnboardingContainer(
    private val permissionController: PermissionController,
) : ViewModel(),
    ContainerHost<OnboardingState, OnboardingSideEffect> {
    override val container: Container<OnboardingState, OnboardingSideEffect> =
        container(OnboardingState()) { refreshPermissions() }

    public fun onIntent(intent: OnboardingIntent) {
        when (intent) {
            OnboardingIntent.RequestPhotoPermission -> requestPhotoPermission()
            OnboardingIntent.AcknowledgeCameraGeolocation -> acknowledgeCameraGeolocation()
            OnboardingIntent.AcknowledgeShareSetup -> acknowledgeShareSetup()
            OnboardingIntent.RequestNotificationPermission -> requestNotificationPermission()
            // Delegates directly to the platform seam rather than round-tripping through a side
            // effect - the same directness GoogleAuthProvider uses for Activity-bound work.
            OnboardingIntent.OpenAppSettings -> permissionController.openAppSettings()
            OnboardingIntent.RefreshPermissions -> intent { refreshPermissions() }
        }
    }

    // Re-queries (never requests) both permissions - used both as the onCreate hook and, fired
    // from the Screen on ON_RESUME, as the mechanism that notices a permission granted from the
    // system Settings screen while the user was away.
    private suspend fun Syntax<OnboardingState, OnboardingSideEffect>.refreshPermissions() {
        reduce {
            state.copy(
                photoPermission = permissionController.status(OnboardingPermission.PHOTOS),
                notificationPermission = permissionController.status(OnboardingPermission.NOTIFICATIONS),
            )
        }
        completeIfDone()
    }

    private fun requestPhotoPermission() =
        intent {
            val status = awaitPermissionRequest(OnboardingPermission.PHOTOS)
            reduce { state.copy(photoPermission = status) }
            completeIfDone()
        }

    private fun requestNotificationPermission() =
        intent {
            val status = awaitPermissionRequest(OnboardingPermission.NOTIFICATIONS)
            reduce { state.copy(notificationPermission = status) }
            completeIfDone()
        }

    private fun acknowledgeCameraGeolocation() =
        intent {
            reduce { state.copy(cameraGeolocationAcknowledged = true) }
            completeIfDone()
        }

    private fun acknowledgeShareSetup() =
        intent {
            reduce { state.copy(shareSetupAcknowledged = true) }
            completeIfDone()
        }

    private suspend fun Syntax<OnboardingState, OnboardingSideEffect>.completeIfDone() {
        if (state.currentStep == null) {
            postSideEffect(OnboardingSideEffect.Completed)
        }
    }

    private suspend fun awaitPermissionRequest(permission: OnboardingPermission): PermissionStatus =
        suspendCancellableCoroutine { continuation ->
            permissionController.request(permission) { status -> continuation.resume(status) }
        }
}
