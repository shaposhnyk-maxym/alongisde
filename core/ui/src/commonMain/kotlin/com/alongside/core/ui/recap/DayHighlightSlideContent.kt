package com.alongside.core.ui.recap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alongside.core.model.recap.RecapSlide
import com.alongside.core.ui.component.AsyncPhotoBanner
import com.alongside.core.ui.theme.AlongsideSpacing
import com.alongside.core.ui.theme.AlongsideTheme
import kotlinx.datetime.LocalDate

private val ScrimBrush =
    Brush.verticalGradient(
        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)),
    )

/**
 * One trip day's highlight (docs/roadmap.md M20.3.5): first available photo across both sides'
 * episodes on this date, headline "Day N" (+ city if any episode has one), caption quoting the
 * first non-null episode description found (own side checked first).
 */
@Composable
public fun DayHighlightSlideContent(
    slide: RecapSlide.DayHighlight,
    modifier: Modifier = Modifier,
) {
    val episodes = slide.ownEpisodes + slide.partnerEpisodes
    val photo = episodes.flatMap { it.photos }.firstOrNull()
    val city = episodes.firstNotNullOfOrNull { it.city }
    val quote = episodes.firstNotNullOfOrNull { it.description }

    Box(modifier = modifier.fillMaxSize()) {
        AsyncPhotoBanner(
            model = photo?.loadableModel(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
        )
        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(ScrimBrush)
                    .padding(AlongsideSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(AlongsideSpacing.sm),
        ) {
            Text(
                text = if (city != null) "Day ${slide.dayIndex} · $city" else "Day ${slide.dayIndex}",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
            )
            quote?.let {
                Text(
                    text = "\"$it\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f),
                )
            }
        }
    }
}

@Preview
@Composable
private fun DayHighlightSlideContentPreview() {
    AlongsideTheme {
        Box(Modifier.size(360.dp, 780.dp)) {
            DayHighlightSlideContent(
                RecapSlide.DayHighlight(
                    date = LocalDate(2026, 7, 20),
                    dayIndex = 3,
                    ownEpisodes =
                        listOf(
                            recapPreviewEpisode(
                                "own-1",
                                description = "Lake at sunset, then the best cherry dumplings of the whole trip.",
                                city = "Ternopil",
                            ),
                        ),
                    partnerEpisodes = emptyList(),
                ),
            )
        }
    }
}
