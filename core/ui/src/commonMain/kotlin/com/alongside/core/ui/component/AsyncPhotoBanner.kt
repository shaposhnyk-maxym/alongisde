package com.alongside.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import com.alongside.core.ui.animation.shimmer
import com.alongside.core.ui.theme.AlongsideTheme
import com.alongside.core.ui.theme.alongsideColors

/**
 * A flexible-size photo banner filling whatever [modifier] gives it - the same loading-shimmer/
 * error-tile handling as [AsyncPhotoTile], but without that component's forced square `.size()`,
 * for full-bleed uses like a swipeable place card.
 */
@Composable
public fun AsyncPhotoBanner(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    SubcomposeAsyncImage(
        model = model,
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier,
    ) {
        when (painter.state.value) {
            is AsyncImagePainter.State.Loading -> Box(Modifier.fillMaxSize().shimmer())
            is AsyncImagePainter.State.Error ->
                Box(Modifier.fillMaxSize().background(MaterialTheme.alongsideColors.iconTileOnPaper))
            else -> SubcomposeAsyncImageContent()
        }
    }
}

@Preview
@Composable
private fun AsyncPhotoBannerPreview() {
    AlongsideTheme {
        AsyncPhotoBanner(
            model = null,
            contentDescription = null,
            modifier = Modifier.size(280.dp, 360.dp),
        )
    }
}
