package com.alongside.feature.onboarding.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.alongside.core.ui.component.DotBanner
import com.alongside.core.ui.component.InkGradientBackground
import com.alongside.feature.onboarding.OnboardingStep
import org.orbitmvi.orbit.compose.collectAsState

@Composable
public fun OnboardingScreen(
    container: OnboardingContainer,
    modifier: Modifier = Modifier,
) {
    val state by container.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    container.onIntent(OnboardingIntent.RefreshPermissions)
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    OnboardingContent(
        state = state,
        onRequestPhotoPermission = { container.onIntent(OnboardingIntent.RequestPhotoPermission) },
        onAcknowledgeCameraGeolocation = { container.onIntent(OnboardingIntent.AcknowledgeCameraGeolocation) },
        onAcknowledgeShareSetup = { container.onIntent(OnboardingIntent.AcknowledgeShareSetup) },
        onRequestNotificationPermission = { container.onIntent(OnboardingIntent.RequestNotificationPermission) },
        onOpenAppSettings = { container.onIntent(OnboardingIntent.OpenAppSettings) },
        modifier = modifier,
    )
}

@Composable
internal fun OnboardingContent(
    state: OnboardingState,
    onRequestPhotoPermission: () -> Unit,
    onAcknowledgeCameraGeolocation: () -> Unit,
    onAcknowledgeShareSetup: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    modifier: Modifier = Modifier,
    animateEntrance: Boolean = true,
) {
    val step = state.currentStep

    InkGradientBackground(modifier = modifier.fillMaxSize()) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .semantics {
                        contentDescription = "onboarding-step-${step?.name ?: "COMPLETE"}"
                    },
        ) {
            when (step) {
                OnboardingStep.PHOTO_PERMISSION ->
                    PhotoPermissionStep(
                        status = state.photoPermission,
                        onRequest = onRequestPhotoPermission,
                        onOpenAppSettings = onOpenAppSettings,
                        animateEntrance = animateEntrance,
                    )
                OnboardingStep.CAMERA_GEOLOCATION ->
                    CameraGeolocationStep(
                        onAcknowledge = onAcknowledgeCameraGeolocation,
                        onOpenAppSettings = onOpenAppSettings,
                        animateEntrance = animateEntrance,
                    )
                OnboardingStep.SHARE_SETUP ->
                    ShareSetupStep(
                        onContinue = onAcknowledgeShareSetup,
                        animateEntrance = animateEntrance,
                    )
                OnboardingStep.NOTIFICATION_PERMISSION ->
                    NotificationPermissionStep(
                        status = state.notificationPermission,
                        onRequest = onRequestNotificationPermission,
                        onOpenAppSettings = onOpenAppSettings,
                    )
                null -> CompletedContent()
            }
        }
    }
}

@Composable
private fun CompletedContent() {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .semantics { contentDescription = "onboarding-complete" },
        contentAlignment = Alignment.Center,
    ) {
        DotBanner(text = "You're all set")
    }
}
