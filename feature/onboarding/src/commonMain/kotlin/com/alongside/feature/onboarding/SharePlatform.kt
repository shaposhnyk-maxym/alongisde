package com.alongside.feature.onboarding

/**
 * Which platform's share sheet the [SHARE_SETUP][OnboardingStep.SHARE_SETUP] step should explain -
 * a plain composable parameter (not `expect`/`actual`) deliberately, since Roborazzi screenshot
 * tests only ever execute on the Android/Robolectric host target: an `iosMain actual` composable
 * would be physically unreachable from that test, making it impossible to golden-test iOS-only UI.
 * Keeping both platforms' content in `commonMain`, gated by an ordinary value, is what makes the
 * iOS-specific Share Extension step screenshot-testable at all (docs/roadmap.md M7).
 */
public enum class SharePlatform {
    ANDROID,
    IOS,
}
