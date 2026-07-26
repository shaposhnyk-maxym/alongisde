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

private val ScrimBrush =
    Brush.verticalGradient(
        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)),
    )

/**
 * "Parallel lives" comparison (docs/roadmap.md M20.3.5): own/partner pre-trip photos side by side,
 * captioned with the haversine distance between them - the real field ([RecapSlide.ParallelLives
 * .distanceMeters]), not a narrated time gap, since that's all `core:domain`'s
 * `selectParallelLivesPair` actually computes.
 */
@Composable
public fun ParallelLivesSlideContent(
    slide: RecapSlide.ParallelLives,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Row(Modifier.fillMaxSize()) {
            AsyncPhotoBanner(
                model = slide.ownPhoto.loadableModel(),
                contentDescription = null,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            Box(Modifier.width(2.dp).fillMaxHeight().background(Color.Black))
            AsyncPhotoBanner(
                model = slide.partnerPhoto.loadableModel(),
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
            Text(text = "Parallel lives", style = MaterialTheme.typography.headlineSmall, color = Color.White)
            Text(
                text = "${formatRecapDistanceMeters(slide.distanceMeters)} apart, before this trip",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
            )
        }
    }
}

@Preview
@Composable
private fun ParallelLivesSlideContentPreview() {
    AlongsideTheme {
        Box(Modifier.size(360.dp, 780.dp)) {
            ParallelLivesSlideContent(
                RecapSlide.ParallelLives(
                    ownPhoto = recapPreviewPreTripPhoto("own-1"),
                    partnerPhoto = recapPreviewPreTripPhoto("partner-1"),
                    distanceMeters = 482_000.0,
                ),
            )
        }
    }
}
