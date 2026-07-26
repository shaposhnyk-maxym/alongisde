package com.alongside.core.ui.recap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
 * The trip's single closest-together moment (docs/roadmap.md M20.3.5): own/partner photos from
 * the overlapping-time episode pair with the smallest haversine distance, same side-by-side
 * layout as [ParallelLivesSlideContent] since both compare an own/partner photo pair by distance.
 */
@Composable
public fun ClosestMomentSlideContent(
    slide: RecapSlide.ClosestMoment,
    modifier: Modifier = Modifier,
) {
    val ownPhoto = slide.ownEpisode.photos.firstOrNull()
    val partnerPhoto = slide.partnerEpisode.photos.firstOrNull()

    Box(modifier = modifier.fillMaxSize()) {
        Row(Modifier.fillMaxSize()) {
            AsyncPhotoBanner(
                model = ownPhoto?.loadableModel(),
                contentDescription = null,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            Box(Modifier.width(2.dp).fillMaxHeight().background(Color.Black))
            AsyncPhotoBanner(
                model = partnerPhoto?.loadableModel(),
                contentDescription = null,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(ScrimBrush)
                    .padding(AlongsideSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(AlongsideSpacing.sm),
        ) {
            Text(text = "Closest moment", style = MaterialTheme.typography.headlineSmall, color = Color.White)
            Text(
                text = "${formatRecapDistanceMeters(slide.distanceMeters)} apart, ${slide.date}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
            )
        }
    }
}

@Preview
@Composable
private fun ClosestMomentSlideContentPreview() {
    AlongsideTheme {
        Box(Modifier.size(360.dp, 780.dp)) {
            ClosestMomentSlideContent(
                RecapSlide.ClosestMoment(
                    date = LocalDate(2026, 7, 22),
                    ownEpisode = recapPreviewEpisode("own-2", city = "Kyiv"),
                    partnerEpisode = recapPreviewEpisode("partner-2", city = "Kyiv"),
                    distanceMeters = 40.0,
                ),
            )
        }
    }
}
