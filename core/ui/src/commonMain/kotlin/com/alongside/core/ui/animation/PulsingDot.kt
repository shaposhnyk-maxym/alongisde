package com.alongside.core.ui.animation

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.alongside.core.ui.theme.AlongsideSpacing
import com.alongside.core.ui.theme.AlongsideTheme
import com.alongside.core.ui.theme.alongsideColors
import com.alongside.core.ui.theme.alongsideTypography

private const val PULSE_DURATION_MILLIS = 900
private const val PULSE_MIN_ALPHA = 0.4f
private const val PULSE_MAX_SCALE = 1.25f

/** Softly pulsing status dot ("Waiting for your partner to join..."). */
@Composable
public fun PulsingDot(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    size: Dp = 8.dp,
) {
    val transition = rememberInfiniteTransition()
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = PULSE_MIN_ALPHA,
        animationSpec = infiniteRepeatable(tween(PULSE_DURATION_MILLIS), RepeatMode.Reverse),
    )
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = PULSE_MAX_SCALE,
        animationSpec = infiniteRepeatable(tween(PULSE_DURATION_MILLIS), RepeatMode.Reverse),
    )
    Box(
        modifier =
            modifier
                .size(size)
                .graphicsLayer {
                    this.alpha = alpha
                    scaleX = scale
                    scaleY = scale
                }
                .background(color, CircleShape)
                .testTag("pulsing-dot"),
    )
}

@Preview
@Composable
private fun PulsingDotPreview() {
    AlongsideTheme {
        Row(
            horizontalArrangement = Arrangement.spacedBy(AlongsideSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PulsingDot()
            Text(
                text = "Waiting for your partner to join...",
                style = MaterialTheme.alongsideTypography.meta,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
