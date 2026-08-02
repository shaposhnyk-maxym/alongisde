package com.alongside.feature.diary.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.alongside.core.domain.diary.DayUnlockState
import com.alongside.core.ui.component.AlongsidePrimaryButton
import com.alongside.core.ui.component.AlongsideTextButton
import com.alongside.core.ui.component.InkGradientBackground
import com.alongside.core.ui.component.PagerDots
import com.alongside.core.ui.component.ScreenHeader
import com.alongside.core.ui.theme.AlongsideSpacing
import com.alongside.core.ui.theme.alongsideColors
import kotlinx.datetime.LocalDate
import org.orbitmvi.orbit.compose.collectAsState

@Composable
public fun DiaryTimelineScreen(
    container: DiaryTimelineContainer,
    modifier: Modifier = Modifier,
    onAddPhotos: (LocalDate) -> Unit = {},
    onAddPreTripPhotos: () -> Unit = {},
) {
    val state by container.collectAsState()
    DiaryTimelineContent(
        items = state.items,
        today = state.today,
        modifier = modifier,
        onAddPhotos = onAddPhotos,
        onCloseDay = { date -> container.onIntent(DiaryTimelineIntent.CloseDay(date)) },
        onAddPreTripPhotos = onAddPreTripPhotos,
        onFlushPreTripPhotoSync = { container.onIntent(DiaryTimelineIntent.FlushPreTripPhotoSync) },
    )
}

private val PagerHorizontalPeek = 32.dp
private val PagerContentPadding = PaddingValues(horizontal = PagerHorizontalPeek)

@Composable
internal fun DiaryTimelineContent(
    items: List<DiaryTimelineItem>,
    modifier: Modifier = Modifier,
    today: LocalDate? = null,
    onAddPhotos: (LocalDate) -> Unit = {},
    onCloseDay: (LocalDate) -> Unit = {},
    onAddPreTripPhotos: () -> Unit = {},
    onFlushPreTripPhotoSync: () -> Unit = {},
) {
    var showContinueCaptureDialog by remember { mutableStateOf(false) }
    val pagerState = rememberPagerState(pageCount = { items.size })
    val selectedItem = items.getOrNull(pagerState.currentPage)
    val selectedDay = (selectedItem as? DiaryTimelineItem.Day)?.card

    InkGradientBackground(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(title = "Timeline")
            Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                if (items.isNotEmpty()) {
                    HorizontalPager(
                        state = pagerState,
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .testTag("timeline-pager"),
                        contentPadding = PagerContentPadding,
                        pageSpacing = AlongsideSpacing.md,
                    ) { page ->
                        DiaryTimelineItemCard(
                            item = items[page],
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .padding(vertical = AlongsideSpacing.xxl)
                                    .testTag("timeline-page-$page"),
                        )
                    }
                    PagerDots(
                        pageCount = items.size,
                        selectedPage = pagerState.currentPage,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .padding(bottom = AlongsideSpacing.xxl),
                    )
                }
                // Once a day is UNLOCKED there's nothing left to add or close - both sides'
                // episodes are already fully revealed, so the capture UI would just be a stale
                // distraction. Only today's own card shows capture UI (docs/roadmap.md M21.2) -
                // a day whose date has already passed (docs/roadmap.md M12.12) is hidden since
                // MISSED is computed, not stored, and backdating a capture into it would just
                // flip it back to READY; a future day is hidden too, since the trip hasn't
                // reached it yet. `today == null` (state not loaded yet) hides every day, since
                // `items` is only ever non-empty once `today` is known too. The Countdown card
                // has no lock/unlock/date concept of its own (docs/roadmap.md M19.8) - its button
                // area always shows while it's the selected page.
                if (selectedDay != null &&
                    selectedDay.unlockState == DayUnlockState.LOCKED &&
                    selectedDay.date == today
                ) {
                    CaptureButtonArea(
                        closedLabel = if (selectedDay.ownClosedAt != null) "This day's entry is closed" else null,
                        showCloseButton = selectedDay.ownEpisodes.isNotEmpty(),
                        onAddPhotosClick = {
                            if (selectedDay.ownEpisodes.isNotEmpty()) {
                                showContinueCaptureDialog = true
                            } else {
                                onAddPhotos(selectedDay.date)
                            }
                        },
                        onCloseDay = { onCloseDay(selectedDay.date) },
                        modifier =
                            Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = AlongsideSpacing.xxl),
                    )
                } else if (selectedItem is DiaryTimelineItem.Countdown) {
                    CaptureButtonArea(
                        closedLabel = null,
                        showCloseButton = true,
                        onAddPhotosClick = onAddPreTripPhotos,
                        onCloseDay = onFlushPreTripPhotoSync,
                        modifier =
                            Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = AlongsideSpacing.xxl),
                    )
                }
            }
        }
    }

    if (showContinueCaptureDialog && selectedDay != null) {
        ContinueCaptureDialog(
            onContinue = {
                showContinueCaptureDialog = false
                onAddPhotos(selectedDay.date)
            },
        )
    }
}

/**
 * Capture entry point for whichever carousel page is currently centered
 * (docs/roadmap.md M12.6/M19.8) - shared by day cards and the Countdown card, so both drive it
 * through the same primitive params rather than a shared concrete model type: [closedLabel]
 * non-null shows a static label instead of any button (day cards only - a closed day is final and
 * can never be reopened for capture on this side; the Countdown card never has a closed state, so
 * always passes `null`); otherwise "Add Photos" always shows, and "Close Day" shows only when
 * [showCloseButton] is true (day cards: only once own episodes exist; the Countdown card: always,
 * since its "Close Day" is really a manual sync-flush, not a day-closing action). The caller
 * (`DiaryTimelineContent`) already hides a day's whole area once its date has passed
 * (docs/roadmap.md M12.12) - restricting it to the trip's date range too (can't backdate before
 * the trip started) remains a follow-up.
 */
@Composable
private fun CaptureButtonArea(
    closedLabel: String?,
    showCloseButton: Boolean,
    onAddPhotosClick: () -> Unit,
    onCloseDay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        // Floats over whichever card is centered underneath it (usually the cream PaperCard,
        // not the dark ink canvas - see AlongsideOnPaperButton's own note on this) - labelMuted
        // gray reads against either, unlike a color picked for just one of the two.
        closedLabel != null ->
            Text(
                text = closedLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.alongsideColors.labelMuted,
                modifier = modifier.testTag("timeline-day-closed-label"),
            )
        else ->
            Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
                AlongsidePrimaryButton(
                    text = "Add Photos",
                    onClick = onAddPhotosClick,
                    modifier = Modifier.testTag("timeline-add-photos"),
                )
                if (showCloseButton) {
                    Spacer(Modifier.height(AlongsideSpacing.sm))
                    AlongsideTextButton(
                        text = "Close Day",
                        onClick = onCloseDay,
                        modifier = Modifier.testTag("timeline-close-day"),
                    )
                }
            }
    }
}

@Composable
private fun ContinueCaptureDialog(onContinue: () -> Unit) {
    AlertDialog(
        onDismissRequest = onContinue,
        modifier = Modifier.testTag("timeline-continue-capture-dialog"),
        title = { Text("You already have entries for this day!") },
        text = { Text("Keep adding - what you've captured stays a surprise until you both close the day.") },
        confirmButton = {
            TextButton(onClick = onContinue, modifier = Modifier.testTag("timeline-continue-capture-confirm")) {
                Text("Continue")
            }
        },
    )
}
