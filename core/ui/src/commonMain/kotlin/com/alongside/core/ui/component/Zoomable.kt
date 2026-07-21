package com.alongside.core.ui.component

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
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
 *
 * Only claims a gesture (calling [androidx.compose.ui.input.pointer.PointerInputChange.consume])
 * when it's an actual pinch (2+ pointers) or the content is already zoomed in - a plain one-finger
 * drag at 1x scale is left unconsumed so an ancestor `HorizontalPager` (as in
 * [FullscreenPhotoViewer]) still receives it and can swipe pages. The naive
 * `detectTransformGestures`-based version this replaced consumed every drag unconditionally,
 * which silently broke swiping between photos whenever the current page carried this modifier -
 * i.e. always, since [FullscreenPhotoViewer] applies it to whichever page is current.
 */
public fun Modifier.zoomable(state: ZoomableState): Modifier =
    this
        .graphicsLayer {
            scaleX = state.scale
            scaleY = state.scale
            translationX = state.offset.x
            translationY = state.offset.y
        }.pointerInput(state) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                do {
                    val event = awaitPointerEvent()
                    val canceled = event.changes.any { it.isConsumed }
                    if (!canceled && (event.changes.size > 1 || state.scale > MIN_SCALE)) {
                        val zoom = event.calculateZoom()
                        val pan = event.calculatePan()
                        val newScale = (state.scale * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
                        val maxX = ((size.width * (newScale - 1)) / 2).coerceAtLeast(0f)
                        val maxY = ((size.height * (newScale - 1)) / 2).coerceAtLeast(0f)
                        state.scale = newScale
                        state.offset =
                            Offset(
                                (state.offset.x + pan.x * newScale).coerceIn(-maxX, maxX),
                                (state.offset.y + pan.y * newScale).coerceIn(-maxY, maxY),
                            )
                        event.changes.forEach { it.consume() }
                    }
                } while (!canceled && event.changes.any { it.pressed })
            }
        }.pointerInput(state) {
            detectTapGestures(
                onDoubleTap = {
                    if (state.scale > MIN_SCALE) state.reset() else state.scale = DOUBLE_TAP_SCALE
                },
            )
        }
