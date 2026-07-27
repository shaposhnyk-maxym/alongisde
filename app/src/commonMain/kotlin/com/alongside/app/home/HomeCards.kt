package com.alongside.app.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alongside.core.model.place.PlaceCandidate
import com.alongside.core.ui.animation.CountUpText
import com.alongside.core.ui.component.AsyncPhotoTile
import com.alongside.core.ui.component.DigitTile
import com.alongside.core.ui.component.DigitTileTone
import com.alongside.core.ui.component.InkCard
import com.alongside.core.ui.component.MediaListRow
import com.alongside.core.ui.component.OverlineLabel
import com.alongside.core.ui.component.OverlineLabelTone
import com.alongside.core.ui.component.PaperCard
import com.alongside.core.ui.component.RecapRing
import com.alongside.core.ui.theme.AlongsideSpacing
import com.alongside.core.ui.theme.alongsideColors
import com.alongside.core.ui.theme.alongsideTypography

private val RecentMatchTileWidth = 96.dp

@Composable
internal fun HomeNoTripPlaceholder(modifier: Modifier = Modifier) {
    InkCard(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "No active trip yet.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun HomePreTripCard(
    daysUntilReunion: Int,
    modifier: Modifier = Modifier,
) {
    PaperCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            OverlineLabel(text = "Until you meet", tone = OverlineLabelTone.Accent)
            Spacer(Modifier.height(AlongsideSpacing.lg))
            CountUpText(targetValue = daysUntilReunion, style = MaterialTheme.alongsideTypography.digit)
            Spacer(Modifier.height(AlongsideSpacing.sm))
            Text(
                text = if (daysUntilReunion == 1) "day to go" else "days to go",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.alongsideColors.onPaperSecondary,
            )
        }
    }
}

@Composable
internal fun HomeTripDayCard(
    dayIndex: Int,
    totalDays: Int,
    city: String?,
    modifier: Modifier = Modifier,
) {
    InkCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            OverlineLabel(text = "Trip day")
            Spacer(Modifier.height(AlongsideSpacing.md))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AlongsideSpacing.xs),
            ) {
                dayIndex.toString().forEach { digit -> DigitTile(char = digit, tone = DigitTileTone.Accent) }
                Text(
                    text = "/$totalDays",
                    style = MaterialTheme.alongsideTypography.digit,
                    color = MaterialTheme.alongsideColors.iconMuted,
                )
            }
            Spacer(Modifier.height(AlongsideSpacing.sm))
            Text(
                text = "together since day 1" + (city?.let { " · $it" } ?: ""),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun HomeTodayCard(
    summary: HomeTodaySummary,
    dayIndex: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OverlineLabel(text = "Today")
        Spacer(Modifier.height(AlongsideSpacing.sm))
        PaperCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
            MediaListRow(
                imageModels = listOfNotNull(summary.photoModel),
                imageContentDescription = summary.placeName ?: "Day $dayIndex",
                title = "Day $dayIndex" + (summary.placeName?.let { " · $it" } ?: ""),
                subtitle = todayCaption(summary),
            )
        }
    }
}

private fun todayCaption(summary: HomeTodaySummary): String =
    when {
        summary.ownHasPhotos && summary.partnerHasPhotos -> "Both added photos — tap to open"
        summary.ownHasPhotos -> "You added photos — waiting on your partner"
        summary.partnerHasPhotos -> "Your partner added photos — add yours"
        else -> "No photos yet today"
    }

@Composable
internal fun HomeRecentMatchesRow(
    matches: List<PlaceCandidate>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OverlineLabel(text = "Recent matches")
        Spacer(Modifier.height(AlongsideSpacing.sm))
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .clickable(onClick = onClick),
            horizontalArrangement = Arrangement.spacedBy(AlongsideSpacing.md),
        ) {
            matches.forEach { match -> HomeRecentMatchTile(match) }
        }
    }
}

@Composable
private fun HomeRecentMatchTile(
    match: PlaceCandidate,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.width(RecentMatchTileWidth)) {
        AsyncPhotoTile(
            model = match.photos.firstOrNull()?.remoteUrl,
            contentDescription = match.name,
            size = RecentMatchTileWidth,
        )
        Spacer(Modifier.height(AlongsideSpacing.xs))
        Text(
            text = match.name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
        )
    }
}

@Composable
internal fun HomeCompletedTripCard(
    daysTogether: Int,
    isRecapAvailable: Boolean,
    modifier: Modifier = Modifier,
) {
    InkCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$daysTogether days together.",
                style = MaterialTheme.alongsideTypography.displaySerifItalic.copy(fontSize = 26.sp, lineHeight = 30.sp),
            )
            Spacer(Modifier.height(AlongsideSpacing.sm))
            Text(
                text =
                    if (isRecapAvailable) {
                        "Your recap is ready above."
                    } else {
                        "Wrapping up your trip — the recap will be ready soon."
                    },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
internal fun HomeRecapEntryPoint(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AlongsideSpacing.md),
    ) {
        RecapRing(onClick = onClick, contentDescription = "Your recap is ready")
        Text(
            text = "Your recap is ready",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}
