package com.alongside.core.ui.recap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
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
import com.alongside.core.ui.theme.alongsideColors
import com.alongside.core.ui.theme.alongsideTypography

private val BadgeSize = 96.dp
private val AvatarDotSize = 26.dp
private val AvatarOverlap = (-8).dp

/** Deck's closing slide (docs/roadmap.md M20.3.5) - always present, always last. */
@Composable
public fun FinalSlideContent(
    slide: RecapSlide.Final,
    modifier: Modifier = Modifier,
) {
    InkGradientBackground(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(AlongsideSpacing.xxl),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(BadgeSize)
                        .clip(CircleShape)
                        .background(MaterialTheme.alongsideColors.gradientBottom),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = slide.daysTogether.toString(),
                    style = MaterialTheme.alongsideTypography.digit,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = "Days together.",
                modifier = Modifier.padding(top = AlongsideSpacing.lg),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "Home again, already counting down to the next one.",
                modifier = Modifier.padding(top = AlongsideSpacing.xs),
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(modifier = Modifier.padding(top = AlongsideSpacing.lg)) {
                Box(Modifier.size(AvatarDotSize).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                Box(
                    Modifier
                        .offset(x = AvatarOverlap)
                        .size(AvatarDotSize)
                        .clip(CircleShape)
                        .background(MaterialTheme.alongsideColors.paperWhite),
                )
            }
        }
    }
}

@Preview
@Composable
private fun FinalSlideContentPreview() {
    AlongsideTheme {
        Box(Modifier.size(360.dp, 780.dp)) {
            FinalSlideContent(RecapSlide.Final(daysTogether = 7))
        }
    }
}
