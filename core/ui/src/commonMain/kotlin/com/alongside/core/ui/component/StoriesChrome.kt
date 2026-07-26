package com.alongside.core.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alongside.core.ui.theme.AlongsideSpacing
import com.alongside.core.ui.theme.AlongsideTheme

private const val DEFAULT_SLIDE_DURATION_MILLIS = 6000
private const val TRANSITION_SCALE_IN = 0.92f
private const val TRANSITION_SCALE_OUT = 1.05f

/**
 * Generic Instagram-Stories-style container (docs/roadmap.md M20.2): top progress bars
 * ([StorySegmentProgress]), left/right tap zones, an auto-advance timer per slide, and a
 * scale+fade transition between slides. Knows nothing about what it's showing - [content] renders
 * whatever slide `index` is current, given how far through its [slideDurationMillis] budget it is
 * (`elapsedFraction`, 0..1) so photo slides can layer their own parallax/Ken-Burns effect on top
 * without this chrome knowing photos exist.
 *
 * Controlled component - [activeIndex]/[onActiveIndexChange] are lifted out, not owned
 * internally, the same contract as `Slider(value, onValueChange)` - so a future Orbit
 * `Container` (docs/roadmap.md M20.3) can hold the current slide in its own `State` rather than
 * this chrome silently owning navigation.
 *
 * Tapping the left zone on the very first slide, and the right zone advancing past the last one,
 * both resolve through [onFinish]/no-op rather than an out-of-range index - real Stories apps
 * stay put on the first slide and exit on the last.
 */
@Composable
public fun StoriesChrome(
    slideCount: Int,
    activeIndex: Int,
    onActiveIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    slideDurationMillis: Int = DEFAULT_SLIDE_DURATION_MILLIS,
    autoAdvance: Boolean = true,
    onFinish: () -> Unit = {},
    content: @Composable (index: Int, elapsedFraction: Float) -> Unit,
) {
    val progress = remember { Animatable(0f) }

    fun goTo(index: Int) {
        when {
            index < 0 -> Unit
            index >= slideCount -> onFinish()
            else -> onActiveIndexChange(index)
        }
    }

    LaunchedEffect(activeIndex, slideDurationMillis, autoAdvance) {
        if (!autoAdvance) return@LaunchedEffect
        progress.snapTo(0f)
        progress.animateTo(1f, tween(slideDurationMillis, easing = LinearEasing))
        goTo(activeIndex + 1)
    }

    Box(modifier = modifier) {
        AnimatedContent(
            targetState = activeIndex,
            transitionSpec = {
                (fadeIn() + scaleIn(initialScale = TRANSITION_SCALE_IN))
                    .togetherWith(fadeOut() + scaleOut(targetScale = TRANSITION_SCALE_OUT))
            },
            modifier = Modifier.fillMaxSize(),
        ) { index ->
            content(index, if (index == activeIndex) progress.value else 1f)
        }

        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { goTo(activeIndex - 1) }
                        .testTag("stories-tap-left"),
            )
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { goTo(activeIndex + 1) }
                        .testTag("stories-tap-right"),
            )
        }

        StorySegmentProgress(
            segmentCount = slideCount,
            activeSegment = activeIndex,
            activeProgress = progress.value,
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(AlongsideSpacing.md),
        )
    }
}

private val PreviewSlideColors =
    listOf(
        Brush.linearGradient(listOf(Color(0xFFE2764A), Color(0xFF8A3A1F))),
        Brush.linearGradient(listOf(Color(0xFF4A7CE2), Color(0xFF1F3A8A))),
        Brush.linearGradient(listOf(Color(0xFF4AE28A), Color(0xFF1F8A4A))),
        Brush.linearGradient(listOf(Color(0xFFE2C94A), Color(0xFF8A7A1F))),
    )

/**
 * `autoAdvance = false`: a live timer would make this golden non-deterministic (the M20.3.5 PR
 * already hit two CI-only screenshot flakes from animation/async timing) - this captures a fixed,
 * reproducible "2 of 4" state instead of hoping the preview scanner settles the animation the same
 * way locally and in CI.
 */
@Preview
@Composable
private fun StoriesChromePreview() {
    AlongsideTheme {
        Box(Modifier.size(360.dp, 780.dp)) {
            StoriesChrome(
                slideCount = PreviewSlideColors.size,
                activeIndex = 1,
                onActiveIndexChange = {},
                autoAdvance = false,
            ) { index, _ ->
                Box(modifier = Modifier.fillMaxSize().background(PreviewSlideColors[index])) {
                    Text(text = "Slide ${index + 1}", modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
}
