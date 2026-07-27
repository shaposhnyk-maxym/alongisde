package com.alongside.core.ui.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.alongside.core.ui.theme.AlongsideTheme
import com.alongside.core.ui.theme.alongsideColors

private const val PULSE_DURATION_MILLIS = 1400
private const val PULSE_MIN_ALPHA = 0.5f
private const val PULSE_MAX_SCALE = 1.08f
private val RingWidth = 2.dp

/**
 * The recap entry point once a trip's recap is ready - an Instagram-story-style gradient ring
 * around a plain circular tap target. Wraps a glyph, not a photo: [com.alongside.core.model.recap.Recap]
 * carries only `tripId`/`availableAt`, and a cover image would mean re-running the whole
 * on-demand slide-generation pipeline in `feature:recap` just to render a ring.
 */
@Composable
public fun RecapRing(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    pulse: Boolean = true,
) {
    val accent = MaterialTheme.alongsideColors.digitAccent
    val ringBrush = Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.4f), accent))
    val transition = rememberInfiniteTransition()
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (pulse) PULSE_MAX_SCALE else 1f,
        animationSpec = infiniteRepeatable(tween(PULSE_DURATION_MILLIS), RepeatMode.Reverse),
    )
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (pulse) PULSE_MIN_ALPHA else 1f,
        animationSpec = infiniteRepeatable(tween(PULSE_DURATION_MILLIS), RepeatMode.Reverse),
    )

    Box(
        modifier =
            modifier
                .size(size)
                .graphicsLayer {
                    if (pulse) {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
                }.border(width = RingWidth, brush = ringBrush, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            onClick = onClick,
            modifier =
                Modifier
                    .size(size - RingWidth * 2)
                    .semantics { this.contentDescription = contentDescription },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.alongsideColors.digitAccent,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(text = "✦")
            }
        }
    }
}

@Preview
@Composable
private fun RecapRingIdlePreview() {
    AlongsideTheme {
        RecapRing(onClick = {}, contentDescription = "Your recap is ready", pulse = false)
    }
}

@Preview
@Composable
private fun RecapRingPulsingPreview() {
    AlongsideTheme {
        RecapRing(onClick = {}, contentDescription = "Your recap is ready")
    }
}
