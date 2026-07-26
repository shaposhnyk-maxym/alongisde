package com.alongside.core.ui.recap

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alongside.core.model.recap.RecapSlide
import com.alongside.core.ui.component.InkGradientBackground
import com.alongside.core.ui.component.MediaListRow
import com.alongside.core.ui.component.PaperCard
import com.alongside.core.ui.theme.AlongsideSpacing
import com.alongside.core.ui.theme.AlongsideTheme
import com.alongside.core.ui.theme.alongsideTypography

/**
 * Places both sides liked (docs/roadmap.md M20.3.5) - dark-canvas Stories adaptation of the
 * light "Our Matches" main-app screen from the design source (that screen isn't itself a Stories
 * slide, so this isn't a literal copy, just the same [MediaListRow]-per-candidate shape).
 */
@Composable
public fun MatchListSlideContent(
    slide: RecapSlide.MatchList,
    modifier: Modifier = Modifier,
) {
    InkGradientBackground(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(AlongsideSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(AlongsideSpacing.md),
        ) {
            item {
                Text(text = "Your matches", style = MaterialTheme.alongsideTypography.displaySerifItalic)
            }
            items(slide.candidates) { candidate ->
                PaperCard {
                    MediaListRow(
                        imageModels = candidate.photos.map { it.loadableModel() }.ifEmpty { listOf(null) },
                        imageContentDescription = candidate.name,
                        title = candidate.name,
                        subtitle = candidate.category,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun MatchListSlideContentPreview() {
    AlongsideTheme {
        Box(Modifier.size(360.dp, 780.dp)) {
            MatchListSlideContent(
                RecapSlide.MatchList(
                    candidates =
                        listOf(
                            recapPreviewPlaceCandidate("m1", "Lviv Coffee Manufacture", "Coffee shop"),
                            recapPreviewPlaceCandidate("m2", "Roshen Fountain", "Landmark"),
                        ),
                ),
            )
        }
    }
}
