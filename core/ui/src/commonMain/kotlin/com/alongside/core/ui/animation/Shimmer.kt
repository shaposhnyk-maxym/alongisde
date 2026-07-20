package com.alongside.core.ui.animation

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alongside.core.ui.theme.AlongsideTheme
import com.alongside.core.ui.theme.alongsideColors

private const val SHIMMER_DURATION_MILLIS = 1200
private const val SHIMMER_TRAVEL_PX = 1000f
private const val SHIMMER_BAND_WIDTH_PX = 400f

/**
 * The sweeping-gradient "loading" placeholder seen while content streams in (YouTube/Instagram-
 * style skeleton), for use on tiles whose real content (e.g. a network image) isn't ready yet.
 */
@Composable
public fun Modifier.shimmer(): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translate by
        transition.animateFloat(
            initialValue = -SHIMMER_BAND_WIDTH_PX,
            targetValue = SHIMMER_TRAVEL_PX,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(SHIMMER_DURATION_MILLIS, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
            label = "shimmerTranslate",
        )
    val base = MaterialTheme.alongsideColors.iconTileOnPaper
    val brush =
        Brush.linearGradient(
            colors = listOf(base.copy(alpha = 0.6f), base.copy(alpha = 1f), base.copy(alpha = 0.6f)),
            start = Offset(translate, 0f),
            end = Offset(translate + SHIMMER_BAND_WIDTH_PX, 0f),
        )
    return this.background(brush)
}

@Preview
@Composable
private fun ShimmerPreview() {
    AlongsideTheme {
        Box(Modifier.size(120.dp).shimmer())
    }
}
