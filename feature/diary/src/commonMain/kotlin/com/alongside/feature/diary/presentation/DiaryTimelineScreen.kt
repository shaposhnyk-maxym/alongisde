package com.alongside.feature.diary.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
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
import com.alongside.core.ui.theme.AlongsideSpacing
import com.alongside.core.ui.theme.alongsideColors
import kotlinx.datetime.LocalDate
import org.orbitmvi.orbit.compose.collectAsState

@Composable
public fun DiaryTimelineScreen(
    container: DiaryTimelineContainer,
    modifier: Modifier = Modifier,
    onAddPhotos: (LocalDate) -> Unit = {},
) {
    val state by container.collectAsState()
    DiaryTimelineContent(
        items = state.items,
        today = state.today,
        modifier = modifier,
        onAddPhotos = onAddPhotos,
        onCloseDay = { date -> container.onIntent(DiaryTimelineIntent.CloseDay(date)) },
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
) {
    var showContinueCaptureDialog by remember { mutableStateOf(false) }
    val pagerState = rememberPagerState(pageCount = { items.size })
    val selectedDay = (items.getOrNull(pagerState.currentPage) as? DiaryTimelineItem.Day)?.card

    InkGradientBackground(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
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
            // Once a day is UNLOCKED there's nothing left to add or close - both sides' episodes
            // are already fully revealed, so the capture UI would just be a stale distraction.
            // A day whose own date has already passed (docs/roadmap.md M12.12) is hidden too -
            // MISSED is computed, not stored, so backdating a capture into it would just flip it
            // back to READY; `today == null` (state not loaded yet) defaults to showing, not
            // hiding, since `items` is only ever non-empty once `today` is known too.
            val isPastDay = selectedDay != null && today != null && selectedDay.date < today
            if (selectedDay != null && selectedDay.unlockState == DayUnlockState.LOCKED && !isPastDay) {
                CaptureButtonArea(
                    day = selectedDay,
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
 * Capture entry point for whichever day is currently centered in the carousel
 * (docs/roadmap.md M12.6) - what shows here depends entirely on [day]: no own episodes yet ->
 * just "Add Photos"; own episodes already added but not closed -> both "Add Photos" (which now
 * warns before proceeding) and "Close Day"; closed -> neither, a day closed once is final and can
 * never be reopened for capture on this side. The caller (`DiaryTimelineContent`) already hides
 * this whole area once the day's date has passed (docs/roadmap.md M12.12) - restricting it to the
 * trip's date range too (can't backdate before the trip started) remains a follow-up.
 */
@Composable
private fun CaptureButtonArea(
    day: DiaryDayCard,
    onAddPhotosClick: () -> Unit,
    onCloseDay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        // Floats over whichever card is centered underneath it (usually the cream PaperCard,
        // not the dark ink canvas - see AlongsideOnPaperButton's own note on this) - labelMuted
        // gray reads against either, unlike a color picked for just one of the two.
        day.ownClosedAt != null ->
            Text(
                text = "This day's entry is closed",
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
                if (day.ownEpisodes.isNotEmpty()) {
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
