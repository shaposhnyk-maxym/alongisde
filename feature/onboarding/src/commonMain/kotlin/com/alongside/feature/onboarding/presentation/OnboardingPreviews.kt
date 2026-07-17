package com.alongside.feature.onboarding.presentation

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alongside.core.ui.theme.AlongsideTheme
import com.alongside.feature.onboarding.PermissionStatus

private val PreviewSize = Modifier.size(360.dp, 640.dp)

@Composable
private fun OnboardingPreview(state: OnboardingState) {
    AlongsideTheme {
        OnboardingContent(
            state = state,
            onRequestPhotoPermission = {},
            onAcknowledgeCameraGeolocation = {},
            onAcknowledgeShareSetup = {},
            onRequestNotificationPermission = {},
            onOpenAppSettings = {},
            modifier = PreviewSize,
            // Settled end state so the golden captures the finished layout, not the blank
            // pre-reveal frame of the entrance animation.
            animateEntrance = false,
        )
    }
}

@Preview
@Composable
private fun OnboardingPhotoPermissionNotDeterminedPreview() {
    OnboardingPreview(OnboardingState())
}

@Preview
@Composable
private fun OnboardingPhotoPermissionDeniedPreview() {
    OnboardingPreview(OnboardingState(photoPermission = PermissionStatus.DENIED))
}

@Preview
@Composable
private fun OnboardingPhotoPermissionDeniedPermanentlyPreview() {
    OnboardingPreview(OnboardingState(photoPermission = PermissionStatus.DENIED_PERMANENTLY))
}

@Preview
@Composable
private fun OnboardingCameraGeolocationPreview() {
    OnboardingPreview(OnboardingState(photoPermission = PermissionStatus.GRANTED))
}

@Preview
@Composable
private fun OnboardingShareSetupPreview() {
    OnboardingPreview(
        OnboardingState(photoPermission = PermissionStatus.GRANTED, cameraGeolocationAcknowledged = true),
    )
}

@Preview
@Composable
private fun OnboardingNotificationPermissionNotDeterminedPreview() {
    OnboardingPreview(
        OnboardingState(
            photoPermission = PermissionStatus.GRANTED,
            cameraGeolocationAcknowledged = true,
            shareSetupAcknowledged = true,
        ),
    )
}

@Preview
@Composable
private fun OnboardingNotificationPermissionDeniedPreview() {
    OnboardingPreview(
        OnboardingState(
            photoPermission = PermissionStatus.GRANTED,
            cameraGeolocationAcknowledged = true,
            shareSetupAcknowledged = true,
            notificationPermission = PermissionStatus.DENIED,
        ),
    )
}

@Preview
@Composable
private fun OnboardingNotificationPermissionDeniedPermanentlyPreview() {
    OnboardingPreview(
        OnboardingState(
            photoPermission = PermissionStatus.GRANTED,
            cameraGeolocationAcknowledged = true,
            shareSetupAcknowledged = true,
            notificationPermission = PermissionStatus.DENIED_PERMANENTLY,
        ),
    )
}
