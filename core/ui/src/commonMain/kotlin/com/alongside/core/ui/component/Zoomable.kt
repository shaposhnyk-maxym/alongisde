package com.alongside.core.ui.component

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput

private const val MIN_SCALE = 1f
private const val MAX_SCALE = 5f
private const val DOUBLE_TAP_SCALE = 2.5f

/** Pinch/double-tap zoom state for one [Modifier.zoomable] target - one per zoomable image. */
@Stable
public class ZoomableState {
    public var scale: Float by mutableFloatStateOf(MIN_SCALE)
        internal set
    public var offset: Offset by mutableStateOf(Offset.Zero)
        internal set

    public fun reset() {
        scale = MIN_SCALE
        offset = Offset.Zero
    }
}

@Composable
public fun rememberZoomableState(): ZoomableState = remember { ZoomableState() }

/**
 * Pinch-to-zoom + drag-to-pan + double-tap-to-toggle-zoom, clamped so the content can never be
 * panned past its own edges. Not centroid-anchored (zooming always expands from the content's own
 * center, not the pinch focal point) - a deliberately simpler, still-natural-feeling
 * implementation rather than the fully rigorous focal-point math a dedicated zoom library would do.
 */
public fun Modifier.zoomable(state: ZoomableState): Modifier =
    this
        .graphicsLayer {
            scaleX = state.scale
            scaleY = state.scale
            translationX = state.offset.x
            translationY = state.offset.y
        }.pointerInput(state) {
            detectTransformGestures { _, pan, zoom, _ ->
                val newScale = (state.scale * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
                val maxX = ((size.width * (newScale - 1)) / 2).coerceAtLeast(0f)
                val maxY = ((size.height * (newScale - 1)) / 2).coerceAtLeast(0f)
                state.scale = newScale
                state.offset =
                    Offset(
                        (state.offset.x + pan.x * newScale).coerceIn(-maxX, maxX),
                        (state.offset.y + pan.y * newScale).coerceIn(-maxY, maxY),
                    )
            }
        }.pointerInput(state) {
            detectTapGestures(
                onDoubleTap = {
                    if (state.scale > MIN_SCALE) state.reset() else state.scale = DOUBLE_TAP_SCALE
                },
            )
        }
