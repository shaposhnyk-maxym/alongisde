@file:OptIn(ExperimentalRoborazziApi::class)

package com.alongside.screenshot

import com.dropbox.differ.SimpleImageComparator
import com.github.takahirom.roborazzi.AndroidComposePreviewTester
import com.github.takahirom.roborazzi.ComposePreviewTester
import com.github.takahirom.roborazzi.ComposePreviewTester.TestParameter.JUnit4TestParameter.AndroidPreviewJUnit4TestParameter
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.ThresholdValidator

/**
 * Skia anti-aliasing differs between macOS/arm64 (where goldens are recorded)
 * and the ubuntu/x86_64 CI runners: rounded corners, circles and font edges
 * land 1-2px differently. Measured against real CI renders the drift peaks at
 * ~4% of pixels on tiny images that are mostly curved edges (PagerDots at
 * 186x17), while a genuine visual regression changes far more.
 *
 * Wired into the generated preview tests via
 * `generateComposePreviewRobolectricTests.testerQualifiedClassName` in
 * RoborazziConventionPlugin, which also injects this source dir into every
 * module's androidHostTest.
 */
class ToleranceComposePreviewTester :
    ComposePreviewTester<AndroidPreviewJUnit4TestParameter> by AndroidComposePreviewTester(
        AndroidComposePreviewTester.Capturer { parameter ->
            AndroidComposePreviewTester.DefaultCapturer().capture(
                parameter.copy(
                    roborazziOptions =
                        parameter.roborazziOptions.copy(
                            compareOptions =
                                parameter.roborazziOptions.compareOptions.copy(
                                    // Absorb per-pixel color drift up to ~5/255 per channel...
                                    imageComparator = SimpleImageComparator(maxDistance = 0.02f),
                                    // ...and allow up to 5% of pixels to still differ.
                                    resultValidator = ThresholdValidator(0.05f),
                                ),
                        ),
                ),
            )
        },
    )
