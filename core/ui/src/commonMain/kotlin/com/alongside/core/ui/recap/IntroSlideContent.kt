package com.alongside.core.ui.recap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alongside.core.model.recap.RecapSlide
import com.alongside.core.ui.component.InkGradientBackground
import com.alongside.core.ui.theme.AlongsideSpacing
import com.alongside.core.ui.theme.AlongsideTheme
import com.alongside.core.ui.theme.alongsideTypography

/**
 * Recap deck's opening slide (docs/roadmap.md M20.3.5) - always present, even with an empty
 * [RecapSlide.Intro.cities]. Fills the whole slide area; the top progress bar/tap zones are
 * layered on separately by M20.2's StoriesChrome, not rendered here.
 */
@Composable
public fun IntroSlideContent(
    slide: RecapSlide.Intro,
    modifier: Modifier = Modifier,
) {
    InkGradientBackground(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(AlongsideSpacing.xxl),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Your trip,\ntogether",
                style = MaterialTheme.alongsideTypography.displaySerifItalic,
            )
            if (slide.cities.isNotEmpty()) {
                Column(
                    modifier = Modifier.padding(top = AlongsideSpacing.xxl),
                    verticalArrangement = Arrangement.spacedBy(AlongsideSpacing.sm),
                ) {
                    slide.cities.forEach { city ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                            )
                            Text(
                                text = city,
                                modifier = Modifier.padding(start = AlongsideSpacing.sm),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun IntroSlideContentPreview() {
    AlongsideTheme {
        Box(Modifier.size(360.dp, 780.dp)) {
            IntroSlideContent(RecapSlide.Intro(cities = listOf("Rzeszów", "Lviv", "Ternopil", "Kyiv")))
        }
    }
}

@Preview
@Composable
private fun IntroSlideContentEmptyCitiesPreview() {
    AlongsideTheme {
        Box(Modifier.size(360.dp, 780.dp)) {
            IntroSlideContent(RecapSlide.Intro(cities = emptyList()))
        }
    }
}
