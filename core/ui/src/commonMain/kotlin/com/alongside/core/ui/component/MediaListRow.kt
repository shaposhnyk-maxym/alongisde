package com.alongside.core.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.alongside.core.ui.theme.AlongsideSpacing
import com.alongside.core.ui.theme.AlongsideTheme
import com.alongside.core.ui.theme.alongsideColors

private val ThumbnailSize = 64.dp

/**
 * A leading-photo list row: thumbnail, title (optionally with a muted-gray accent appended after
 * an em dash, e.g. a rating), optional muted subtitle. Deliberately generic, not tied to any one
 * feature - `feature:places`' place rows are the first consumer, but the shape (media + title +
 * accent + subtitle) is exactly what a future Matcher Match-list row needs too. Not wrapped in
 * [PaperCard] itself, so callers can place it in whatever card/surface fits their screen.
 *
 * Tapping the thumbnail opens [FullscreenPhotoViewer] over every entry in [imageModels] (not just
 * the leading one shown in the row), same "tap a tile, swipe through the rest" convention
 * `feature:diary`'s `EpisodePhotoGallery` already established - self-contained here too, so
 * callers don't need to own any dialog-visibility state themselves.
 */
@Composable
public fun MediaListRow(
    imageModels: List<Any?>,
    imageContentDescription: String?,
    title: String,
    modifier: Modifier = Modifier,
    titleAccent: String? = null,
    subtitle: String? = null,
) {
    var isGalleryOpen by remember { mutableStateOf(false) }

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        AsyncPhotoTile(
            model = imageModels.firstOrNull(),
            contentDescription = imageContentDescription,
            size = ThumbnailSize,
            onClick = if (imageModels.isNotEmpty()) ({ isGalleryOpen = true }) else null,
        )
        Spacer(Modifier.width(AlongsideSpacing.md))
        Column {
            Text(
                text = titledText(title, titleAccent),
                style = MaterialTheme.typography.titleMedium,
            )
            subtitle?.let {
                Spacer(Modifier.height(AlongsideSpacing.xs))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.alongsideColors.onPaperSecondary,
                )
            }
        }
    }

    if (isGalleryOpen && imageModels.isNotEmpty()) {
        Dialog(
            onDismissRequest = { isGalleryOpen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            FullscreenPhotoViewer(
                models = imageModels,
                initialIndex = 0,
                onDismissRequest = { isGalleryOpen = false },
            )
        }
    }
}

@Composable
private fun titledText(
    title: String,
    titleAccent: String?,
) = buildAnnotatedString {
    append(title)
    if (titleAccent != null) {
        append(" — ")
        withStyle(SpanStyle(color = MaterialTheme.alongsideColors.onPaperSecondary)) {
            append(titleAccent)
        }
    }
}

@Preview
@Composable
private fun MediaListRowWithRatingAndCategoryPreview() {
    AlongsideTheme {
        PaperCard {
            MediaListRow(
                imageModels = listOf(null),
                imageContentDescription = "Lviv Coffee Manufacture",
                title = "Lviv Coffee Manufacture",
                titleAccent = "4.6",
                subtitle = "Coffee shop",
            )
        }
    }
}

@Preview
@Composable
private fun MediaListRowWithoutRatingPreview() {
    AlongsideTheme {
        PaperCard {
            MediaListRow(
                imageModels = listOf(null),
                imageContentDescription = "Rynok Square",
                title = "Rynok Square",
                subtitle = "Landmark",
            )
        }
    }
}

@Preview
@Composable
private fun MediaListRowWithoutSubtitlePreview() {
    AlongsideTheme {
        PaperCard {
            MediaListRow(
                imageModels = listOf(null),
                imageContentDescription = "Roshen Fountain",
                title = "Roshen Fountain",
                titleAccent = "4.8",
            )
        }
    }
}
