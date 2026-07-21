package com.alongside.feature.diary.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.alongside.core.domain.diary.DayUnlockState
import com.alongside.core.model.diary.Episode
import com.alongside.core.model.diary.Photo
import com.alongside.core.ui.animation.CountUpText
import com.alongside.core.ui.animation.PulsingDot
import com.alongside.core.ui.animation.TypewriterText
import com.alongside.core.ui.component.AsyncPhotoTile
import com.alongside.core.ui.component.FullscreenPhotoViewer
import com.alongside.core.ui.component.OverlineLabel
import com.alongside.core.ui.component.OverlineLabelTone
import com.alongside.core.ui.component.PaperCard
import com.alongside.core.ui.format.countryCodeToFlagEmoji
import com.alongside.core.ui.theme.AlongsideSpacing
import com.alongside.core.ui.theme.alongsideColors
import com.alongside.core.ui.theme.alongsideTypography
import kotlinx.coroutines.delay

@Composable
internal fun DiaryTimelineItemCard(
    item: DiaryTimelineItem,
    modifier: Modifier = Modifier,
    animateEntrance: Boolean = true,
) {
    when (item) {
        is DiaryTimelineItem.Countdown -> CountdownCard(item.daysUntilReunion, modifier, animateEntrance)
        is DiaryTimelineItem.Day -> DiaryDayCardContent(item.card, modifier, animateEntrance)
    }
}

@Composable
internal fun CountdownCard(
    daysUntilReunion: Int,
    modifier: Modifier = Modifier,
    animateEntrance: Boolean = true,
) {
    PaperCard(
        modifier =
            modifier
                .fillMaxSize()
                .semantics { contentDescription = "timeline-countdown" },
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            OverlineLabel(text = "Until you meet", tone = OverlineLabelTone.Accent)
            Spacer(Modifier.height(AlongsideSpacing.lg))
            CountUpText(
                targetValue = daysUntilReunion,
                style = MaterialTheme.alongsideTypography.digit,
                // Settled screenshots start already at the target - see StaggerRevealColumn's
                // initiallyRevealed for why the scanner needs the finished frame, not frame zero.
                startValue = if (animateEntrance) 0 else daysUntilReunion,
            )
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
internal fun DiaryDayCardContent(
    card: DiaryDayCard,
    modifier: Modifier = Modifier,
    animateEntrance: Boolean = true,
) {
    when (card.unlockState) {
        DayUnlockState.LOCKED -> {
            val waitingState = checkNotNull(card.waitingState) { "a locked day always has a waiting reason" }
            LockedDayCard(card.dayIndex, waitingState, modifier)
        }
        DayUnlockState.UNLOCKED -> UnlockedDayCard(card, modifier, animateEntrance)
    }
}

@Composable
private fun LockedDayCard(
    dayIndex: Int,
    waitingState: DiaryDayWaitingState,
    modifier: Modifier = Modifier,
) {
    PaperCard(
        modifier =
            modifier
                .fillMaxSize()
                .semantics { contentDescription = "timeline-day-$dayIndex-locked-${waitingState.name}" },
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            OverlineLabel(text = "Day $dayIndex", tone = OverlineLabelTone.Accent)
            Spacer(Modifier.height(AlongsideSpacing.lg))
            Text(
                text = waitingState.headline(),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(AlongsideSpacing.md))
            Text(
                text = waitingState.explanation(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.alongsideColors.onPaperSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(AlongsideSpacing.xl))
            PulsingDot()
        }
    }
}

private fun DiaryDayWaitingState.headline(): String =
    when (this) {
        DiaryDayWaitingState.PARTNER_CAPTURING -> "Your partner is still out there"
        DiaryDayWaitingState.WAITING_FOR_SYNC -> "Almost there"
        DiaryDayWaitingState.GENERATING_TEXT -> "Writing today's page"
    }

private fun DiaryDayWaitingState.explanation(): String =
    when (this) {
        DiaryDayWaitingState.PARTNER_CAPTURING ->
            "This day unlocks once you've both closed it out - they haven't captured theirs yet."
        DiaryDayWaitingState.WAITING_FOR_SYNC ->
            "Both sides are in, just waiting for the network to confirm."
        DiaryDayWaitingState.GENERATING_TEXT ->
            "Turning today's photos into a diary entry."
    }

@Composable
private fun UnlockedDayCard(
    card: DiaryDayCard,
    modifier: Modifier = Modifier,
    animateEntrance: Boolean = true,
) {
    PaperCard(
        modifier =
            modifier
                .fillMaxSize()
                .semantics { contentDescription = "timeline-day-${card.dayIndex}-unlocked" },
    ) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            OverlineLabel(text = "Day ${card.dayIndex}", tone = OverlineLabelTone.Accent)
            Spacer(Modifier.height(AlongsideSpacing.lg))
            EpisodeSection(label = "You", episodes = card.ownEpisodes, animateEntrance = animateEntrance)
            if (card.partnerEpisodes.isNotEmpty()) {
                Spacer(Modifier.height(AlongsideSpacing.xl))
                EpisodeSection(
                    label = "Your partner",
                    episodes = card.partnerEpisodes,
                    animateEntrance = animateEntrance,
                )
            }
        }
    }
}

@Composable
private fun EpisodeSection(
    label: String,
    episodes: List<Episode>,
    animateEntrance: Boolean,
) {
    Column {
        OverlineLabel(text = label)
        episodes.forEach { episode ->
            // Anchors each episode's remembered state (fullscreen-viewer selection, stagger-reveal
            // progress) to its own id - without this, a plain forEach reuses Compose's positional
            // slot when the reactive episode list reorders/inserts (new episodes arrive via a
            // live poll), silently handing one episode's remembered state to whichever episode
            // now occupies that same list position.
            key(episode.id) {
                Spacer(Modifier.height(AlongsideSpacing.md))
                episode.placeName?.let { placeName ->
                    val flag = episode.countryCode?.let { " ${countryCodeToFlagEmoji(it)}" }.orEmpty()
                    Text(text = "$placeName$flag", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(AlongsideSpacing.xs))
                }
                episode.description?.let { description ->
                    TypewriterText(
                        text = description,
                        style = MaterialTheme.alongsideTypography.diaryBody,
                        initiallyRevealed = !animateEntrance,
                    )
                    Spacer(Modifier.height(AlongsideSpacing.md))
                }
                EpisodePhotoGallery(photos = episode.photos, initiallyRevealed = !animateEntrance)
            }
        }
    }
}

private val PhotoTileSize = 96.dp
private val PhotoGalleryHeight = PhotoTileSize * 2 + AlongsideSpacing.xs
private const val PHOTO_STAGGER_DELAY_MILLIS = 80L

private fun Photo.loadableModel(): String = remoteUrl ?: uri

/**
 * Reveals [photos] one at a time in list order (docs/roadmap.md M12's stagger-order accept
 * criterion), laid out as a two-row lazy horizontal grid (docs/roadmap.md M12.8/M12.9) that
 * scrolls once there are more photos than fit - keeps index `i` mapped to `photos[i]`, never
 * re-sorted or re-grouped. Each tile loads the real photo via [AsyncPhotoTile] (remote URL if
 * already uploaded, falling back to the local `content://` URI otherwise - Coil accepts either);
 * tapping one opens [FullscreenPhotoViewer] on that photo, swipeable to the rest of the episode.
 */
@Composable
internal fun EpisodePhotoGallery(
    photos: List<Photo>,
    modifier: Modifier = Modifier,
    initiallyRevealed: Boolean = false,
) {
    var revealedCount by remember(photos.size) { mutableIntStateOf(if (initiallyRevealed) photos.size else 0) }
    var fullscreenIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(photos.size, initiallyRevealed) {
        if (initiallyRevealed) return@LaunchedEffect
        revealedCount = 0
        repeat(photos.size) {
            delay(PHOTO_STAGGER_DELAY_MILLIS)
            revealedCount++
        }
    }

    LazyHorizontalGrid(
        rows = GridCells.Fixed(2),
        modifier = modifier.height(PhotoGalleryHeight),
        horizontalArrangement = Arrangement.spacedBy(AlongsideSpacing.xs),
        verticalArrangement = Arrangement.spacedBy(AlongsideSpacing.xs),
    ) {
        items(photos.size, key = { index -> photos[index].id }) { index ->
            val photo = photos[index]
            AnimatedVisibility(
                visible = index < revealedCount,
                enter = fadeIn() + slideInVertically(),
                modifier = Modifier.testTag("episode-photo-${photo.id}"),
            ) {
                AsyncPhotoTile(
                    model = photo.loadableModel(),
                    contentDescription = null,
                    size = PhotoTileSize,
                    onClick = { fullscreenIndex = index },
                    modifier = Modifier.semantics { contentDescription = "photo-${photo.id}" },
                )
            }
        }
    }

    val openIndex = fullscreenIndex
    if (openIndex != null) {
        Dialog(
            onDismissRequest = { fullscreenIndex = null },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            FullscreenPhotoViewer(
                models = photos.map { it.loadableModel() },
                initialIndex = openIndex,
                onDismissRequest = { fullscreenIndex = null },
            )
        }
    }
}
