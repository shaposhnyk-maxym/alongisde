package com.alongside.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import com.alongside.core.ui.animation.shimmer
import com.alongside.core.ui.theme.AlongsideTheme
import com.alongside.core.ui.theme.alongsideColors

private val DefaultTileSize = 96.dp
private val TileShape = RoundedCornerShape(12.dp)

/**
 * A single photo tile that loads [model] (a remote URL or a local `content://` URI - Coil accepts
 * either directly) with a shimmering skeleton while it's in flight, falling back to a plain muted
 * tile if loading fails (e.g. the URL never uploaded, or the local URI's permission expired).
 * [onClick], when set, opens whatever the caller wants on tap (e.g. [FullscreenPhotoViewer]).
 */
@Composable
public fun AsyncPhotoTile(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = DefaultTileSize,
    onClick: (() -> Unit)? = null,
) {
    val clickModifier =
        if (onClick != null) {
            Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onClick,
            )
        } else {
            Modifier
        }
    SubcomposeAsyncImage(
        model = model,
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier =
            modifier
                .size(size)
                .clip(TileShape)
                .then(clickModifier)
                .testTag("async-photo-tile"),
    ) {
        when (painter.state.value) {
            is AsyncImagePainter.State.Loading ->
                Box(Modifier.fillMaxSize().shimmer().testTag("async-photo-tile-loading"))
            is AsyncImagePainter.State.Error ->
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.alongsideColors.iconTileOnPaper)
                        .testTag("async-photo-tile-error"),
                )
            else -> SubcomposeAsyncImageContent()
        }
    }
}

@Preview
@Composable
private fun AsyncPhotoTilePreview() {
    AlongsideTheme {
        AsyncPhotoTile(model = null, contentDescription = null)
    }
}
