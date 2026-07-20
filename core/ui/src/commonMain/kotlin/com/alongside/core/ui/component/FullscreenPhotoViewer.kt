package com.alongside.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import com.alongside.core.ui.animation.shimmer
import com.alongside.core.ui.theme.AlongsideSpacing
import com.alongside.core.ui.theme.AlongsideTheme

private val ScrimColor = Color.Black.copy(alpha = 0.95f)

/**
 * One photo, pinch/double-tap zoomable, resetting its own zoom whenever a different page becomes
 * current - swiping away from a zoomed-in photo and back to it starts fresh rather than staying
 * stuck zoomed in.
 */
@Composable
private fun ZoomableAsyncImage(
    model: Any?,
    contentDescription: String?,
    isCurrentPage: Boolean,
    modifier: Modifier = Modifier,
) {
    val zoomState = rememberZoomableState()
    if (!isCurrentPage && zoomState.scale != 1f) zoomState.reset()

    SubcomposeAsyncImage(
        model = model,
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = modifier.fillMaxSize(),
    ) {
        when (painter.state.value) {
            is AsyncImagePainter.State.Loading -> Box(Modifier.fillMaxSize().shimmer())
            is AsyncImagePainter.State.Error -> {
                // A dedicated error message, not a shimmer/tile fallback - at fullscreen size a
                // silently-empty/still-loading-looking frame would read as the app being stuck.
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Couldn't load this photo", color = Color.White)
                }
            }
            else ->
                SubcomposeAsyncImageContent(
                    modifier = if (isCurrentPage) Modifier.zoomable(zoomState) else Modifier,
                )
        }
    }
}

/**
 * Fullscreen photo viewer - swipe between [models] (a `HorizontalPager`, one entry per photo),
 * pinch/double-tap to zoom the current one. Self-contained: renders its own dark scrim + close
 * button, so the caller only needs to conditionally compose it (e.g. from a tapped thumbnail's
 * `onClick`) rather than host a system dialog/window itself.
 */
@Composable
public fun FullscreenPhotoViewer(
    models: List<Any?>,
    initialIndex: Int,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    pagerState: PagerState = rememberPagerState(initialPage = initialIndex, pageCount = { models.size }),
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(ScrimColor)
                .testTag("fullscreen-photo-viewer"),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            ZoomableAsyncImage(
                model = models[page],
                contentDescription = null,
                isCurrentPage = page == pagerState.currentPage,
                modifier = Modifier.testTag("fullscreen-photo-viewer-page-$page"),
            )
        }
        CircleIconButton(
            onClick = onDismissRequest,
            contentDescription = "Close",
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(AlongsideSpacing.lg)
                    .testTag("fullscreen-photo-viewer-close"),
        ) {
            Text("✕")
        }
        if (models.size > 1) {
            PagerDots(
                pageCount = models.size,
                selectedPage = pagerState.currentPage,
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = AlongsideSpacing.xxl),
            )
        }
    }
}

@Preview
@Composable
private fun FullscreenPhotoViewerPreview() {
    AlongsideTheme {
        FullscreenPhotoViewer(
            models = listOf(null, null, null),
            initialIndex = 0,
            onDismissRequest = {},
            modifier = Modifier.size(360.dp, 640.dp),
        )
    }
}
