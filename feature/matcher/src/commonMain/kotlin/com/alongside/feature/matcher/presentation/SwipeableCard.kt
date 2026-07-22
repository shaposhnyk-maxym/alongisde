package com.alongside.feature.matcher.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.alongside.core.model.place.PlaceCandidate
import com.alongside.core.model.place.SwipeDirection
import com.alongside.core.ui.component.AsyncPhotoBanner
import com.alongside.core.ui.theme.AlongsideSpacing
import com.alongside.core.ui.theme.alongsideColors
import kotlinx.coroutines.launch
import kotlin.math.abs

private val SwipeCommitThreshold = 120.dp
private val FlingDistance = 1200.dp
private val RotationReferenceWidth = 300.dp
private const val MAX_ROTATION_DEGREES = 12f

/**
 * A single Tinder-style card: horizontal drag only, no velocity tracking (a fixed distance
 * threshold is enough for this milestone). The swipe decision is dispatched to [onSwipe] the
 * moment the threshold is crossed at drag-end - the fling-off animation that follows is purely
 * cosmetic and runs independently, so the decision itself never waits on an animation to finish.
 */
@Composable
internal fun SwipeableCard(
    candidate: PlaceCandidate,
    onSwipe: (SwipeDirection) -> Unit,
    modifier: Modifier = Modifier,
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val thresholdPx = with(density) { SwipeCommitThreshold.toPx() }
    val flingDistancePx = with(density) { FlingDistance.toPx() }
    val rotationReferencePx = with(density) { RotationReferenceWidth.toPx() }

    Surface(
        modifier =
            modifier
                .testTag("swipeable-card")
                .graphicsLayer {
                    translationX = offsetX
                    rotationZ =
                        (offsetX / rotationReferencePx * MAX_ROTATION_DEGREES)
                            .coerceIn(-MAX_ROTATION_DEGREES, MAX_ROTATION_DEGREES)
                }.pointerInput(candidate.id) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            offsetX += dragAmount
                        },
                        onDragEnd = {
                            val committed = offsetX
                            if (abs(committed) > thresholdPx) {
                                val direction = if (committed > 0) SwipeDirection.LIKE else SwipeDirection.DISLIKE
                                onSwipe(direction)
                                val target = if (committed > 0) flingDistancePx else -flingDistancePx
                                scope.launch {
                                    Animatable(committed).animateTo(target, tween(200)) { offsetX = value }
                                }
                            } else {
                                scope.launch {
                                    Animatable(committed).animateTo(0f, spring()) { offsetX = value }
                                }
                            }
                        },
                    )
                },
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.alongsideColors.paper,
        contentColor = MaterialTheme.alongsideColors.onPaper,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            AsyncPhotoBanner(
                model = candidate.photos.firstOrNull()?.remoteUrl,
                contentDescription = candidate.name,
                modifier = Modifier.fillMaxWidth().weight(1f),
            )
            Column(modifier = Modifier.padding(AlongsideSpacing.lg)) {
                Text(text = candidate.name, style = MaterialTheme.typography.titleMedium)
                candidate.city?.let { city ->
                    Text(
                        text = city,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.alongsideColors.onPaperSecondary,
                    )
                }
            }
        }
    }
}
