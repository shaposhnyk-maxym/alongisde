package com.alongside.playground

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.alongside.core.ui.animation.CountUpText
import com.alongside.core.ui.animation.PulsingDot
import com.alongside.core.ui.animation.StaggerRevealColumn
import com.alongside.core.ui.animation.TypewriterText
import com.alongside.core.ui.component.AlongsideOnPaperButton
import com.alongside.core.ui.component.AlongsidePrimaryButton
import com.alongside.core.ui.component.AlongsideSecondaryButton
import com.alongside.core.ui.component.AlongsideTextButton
import com.alongside.core.ui.component.CircleIconButton
import com.alongside.core.ui.component.CircleIconButtonStyle
import com.alongside.core.ui.component.DigitTile
import com.alongside.core.ui.component.DigitTileTone
import com.alongside.core.ui.component.DotBanner
import com.alongside.core.ui.component.InkCard
import com.alongside.core.ui.component.InkGradientBackground
import com.alongside.core.ui.component.OverlineLabel
import com.alongside.core.ui.component.OverlineLabelTone
import com.alongside.core.ui.component.PagerDots
import com.alongside.core.ui.component.PaperCard
import com.alongside.core.ui.component.StorySegmentProgress
import com.alongside.core.ui.theme.AlongsideSpacing
import com.alongside.core.ui.theme.AlongsideTheme
import com.alongside.core.ui.theme.alongsideColors
import com.alongside.core.ui.theme.alongsideTypography

private const val STORY_LOOP_MILLIS = 4000
private const val PAGER_LOOP_MILLIS = 5000
private const val PAGER_PAGES = 5

@Composable
private fun Section(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AlongsideSpacing.md)) {
        OverlineLabel(text = title, tone = OverlineLabelTone.Accent)
        content()
    }
}

@Composable
private fun PlaygroundContent() {
    AlongsideTheme {
        InkGradientBackground(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(AlongsideSpacing.xxl),
                verticalArrangement = Arrangement.spacedBy(AlongsideSpacing.xxl),
            ) {
                BrandSection()
                ButtonsSection()
                CardsSection()
                LabelsAndTilesSection()
                BannerSection()
                ProgressSection()
                MatcherSection()
                StatusSection()
                AnimationsSection()
            }
        }
    }
}

@Composable
private fun BrandSection() {
    Column(verticalArrangement = Arrangement.spacedBy(AlongsideSpacing.sm)) {
        Text(text = "Alongside", style = MaterialTheme.alongsideTypography.displaySerifItalic)
        Text(
            text = "One trip. Two paths.\nEvery day a little closer.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ButtonsSection() {
    Section(title = "Buttons") {
        Column(verticalArrangement = Arrangement.spacedBy(AlongsideSpacing.md)) {
            AlongsidePrimaryButton(
                text = "Allow Photo Access",
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
            )
            AlongsideSecondaryButton(
                text = "Copy Code",
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
            )
            AlongsideOnPaperButton(
                text = "Continue with Google",
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
            )
            AlongsideTextButton(text = "Not now", onClick = {})
        }
    }
}

@Composable
private fun CardsSection() {
    Section(title = "Cards") {
        Row(horizontalArrangement = Arrangement.spacedBy(AlongsideSpacing.lg)) {
            PaperCard {
                Text(text = "Day 4 · Vinnytsia", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Both added photos — tap to open",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.alongsideColors.onPaperSecondary,
                )
            }
            InkCard {
                Text(text = "Day 5 isn't ready yet", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Your partner is still out taking photos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LabelsAndTilesSection() {
    Section(title = "Overlines and digit tiles") {
        Column(verticalArrangement = Arrangement.spacedBy(AlongsideSpacing.md)) {
            OverlineLabel(text = "Step 1 of 4", tone = OverlineLabelTone.Accent)
            OverlineLabel(text = "New Matches")
            Row(horizontalArrangement = Arrangement.spacedBy(AlongsideSpacing.sm)) {
                "7F3K9Q".forEach { DigitTile(char = it) }
                DigitTile(char = '4', tone = DigitTileTone.Paper)
            }
        }
    }
}

@Composable
private fun BannerSection() {
    Section(title = "Banner") {
        DotBanner(
            text = "Added \"Rynok Square\" from Google Maps",
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ProgressSection() {
    Section(title = "Story progress and pager dots") {
        val transition = rememberInfiniteTransition()
        val storyProgress by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(STORY_LOOP_MILLIS, easing = LinearEasing)),
        )
        val pagerPosition by transition.animateFloat(
            initialValue = 0f,
            targetValue = PAGER_PAGES.toFloat(),
            animationSpec =
                infiniteRepeatable(
                    tween(PAGER_LOOP_MILLIS, easing = LinearEasing),
                    RepeatMode.Restart,
                ),
        )
        Column(verticalArrangement = Arrangement.spacedBy(AlongsideSpacing.lg)) {
            StorySegmentProgress(
                segmentCount = 4,
                activeSegment = 2,
                modifier = Modifier.width(360.dp),
                activeProgress = storyProgress,
            )
            PagerDots(
                pageCount = PAGER_PAGES,
                selectedPage = pagerPosition.toInt().coerceAtMost(PAGER_PAGES - 1),
            )
        }
    }
}

@Composable
private fun MatcherSection() {
    Section(title = "Matcher actions") {
        Row(
            horizontalArrangement = Arrangement.spacedBy(AlongsideSpacing.xl),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircleIconButton(onClick = {}, contentDescription = "Skip place") {
                Text("✕")
            }
            CircleIconButton(
                onClick = {},
                contentDescription = "Want to go",
                style = CircleIconButtonStyle.Primary,
            ) {
                Text("♥")
            }
        }
    }
}

@Composable
private fun StatusSection() {
    Section(title = "Status") {
        Row(
            horizontalArrangement = Arrangement.spacedBy(AlongsideSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PulsingDot()
            Text(
                text = "Waiting for your partner to join...",
                style = MaterialTheme.alongsideTypography.meta,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AnimationsSection() {
    Section(title = "Animations") {
        Column(verticalArrangement = Arrangement.spacedBy(AlongsideSpacing.lg)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(AlongsideSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OverlineLabel(text = "Trip Day", tone = OverlineLabelTone.Accent)
                CountUpText(
                    targetValue = 7,
                    style = MaterialTheme.alongsideTypography.digit,
                    modifier = Modifier,
                )
            }
            InkCard(modifier = Modifier.width(360.dp)) {
                TypewriterText(
                    text =
                        "We wandered into the old quarter and found a courtyard " +
                            "full of cats and lemonade.",
                    style = MaterialTheme.alongsideTypography.diaryBody,
                    showCursor = true,
                )
            }
            StaggerRevealColumn(itemCount = 3, modifier = Modifier.size(360.dp, 220.dp)) { index ->
                PaperCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Match $index", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

fun main() =
    application {
        Window(onCloseRequest = ::exitApplication, title = "Alongside Playground") {
            PlaygroundContent()
        }
    }
