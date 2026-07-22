package com.alongside.feature.diary.presentation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alongside.core.domain.diary.DayUnlockState
import com.alongside.core.model.SyncStatus
import com.alongside.core.model.diary.Episode
import com.alongside.core.model.diary.Photo
import com.alongside.core.ui.theme.AlongsideTheme
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

private val PreviewSize = Modifier.size(360.dp, 640.dp)

private fun previewPhoto(id: String) =
    Photo(
        id = id,
        uri = "content://photos/$id",
        takenAt = Instant.fromEpochMilliseconds(0),
        latitude = 49.8397,
        longitude = 24.0297,
    )

private fun previewEpisode(
    id: String,
    placeName: String,
    description: String,
    photoCount: Int,
) = Episode(
    id = id,
    diaryEntryId = "entry-$id",
    startTime = Instant.fromEpochMilliseconds(0),
    endTime = Instant.fromEpochMilliseconds(0),
    latitude = 49.8397,
    longitude = 24.0297,
    placeName = placeName,
    description = description,
    descriptionAttempts = 1,
    photos = (1..photoCount).map { previewPhoto("$id-$it") },
    syncStatus = SyncStatus.SYNCED,
    updatedAt = Instant.fromEpochMilliseconds(0),
)

@Composable
private fun ItemPreview(item: DiaryTimelineItem) {
    AlongsideTheme {
        // Settled end state so the golden captures the finished layout, not the blank
        // pre-reveal frame of the entrance animations (same pattern as PairingPreviews).
        DiaryTimelineItemCard(item = item, modifier = PreviewSize, animateEntrance = false)
    }
}

@Preview
@Composable
private fun CountdownPreview() {
    ItemPreview(DiaryTimelineItem.Countdown(daysUntilReunion = 7))
}

@Preview
@Composable
private fun LockedPartnerCapturingPreview() {
    ItemPreview(
        DiaryTimelineItem.Day(
            DiaryDayCard(
                date = LocalDate(2026, 7, 19),
                dayIndex = 3,
                unlockState = DayUnlockState.LOCKED,
                waitingState = DiaryDayWaitingState.PARTNER_CAPTURING,
                ownEpisodes = emptyList(),
                partnerEpisodes = emptyList(),
                ownClosedAt = null,
            ),
        ),
    )
}

@Preview
@Composable
private fun LockedWaitingForSyncPreview() {
    ItemPreview(
        DiaryTimelineItem.Day(
            DiaryDayCard(
                date = LocalDate(2026, 7, 19),
                dayIndex = 3,
                unlockState = DayUnlockState.LOCKED,
                waitingState = DiaryDayWaitingState.WAITING_FOR_SYNC,
                ownEpisodes = emptyList(),
                partnerEpisodes = emptyList(),
                ownClosedAt = null,
            ),
        ),
    )
}

@Preview
@Composable
private fun LockedGeneratingTextPreview() {
    ItemPreview(
        DiaryTimelineItem.Day(
            DiaryDayCard(
                date = LocalDate(2026, 7, 19),
                dayIndex = 3,
                unlockState = DayUnlockState.LOCKED,
                waitingState = DiaryDayWaitingState.GENERATING_TEXT,
                ownEpisodes = emptyList(),
                partnerEpisodes = emptyList(),
                ownClosedAt = null,
            ),
        ),
    )
}

@Preview
@Composable
private fun LockedMissedPreview() {
    ItemPreview(
        DiaryTimelineItem.Day(
            DiaryDayCard(
                date = LocalDate(2026, 7, 19),
                dayIndex = 3,
                unlockState = DayUnlockState.LOCKED,
                waitingState = DiaryDayWaitingState.MISSED,
                ownEpisodes = emptyList(),
                partnerEpisodes = emptyList(),
                ownClosedAt = null,
            ),
        ),
    )
}

@Preview
@Composable
private fun UnlockedDayPreview() {
    ItemPreview(
        DiaryTimelineItem.Day(
            DiaryDayCard(
                date = LocalDate(2026, 7, 19),
                dayIndex = 3,
                unlockState = DayUnlockState.UNLOCKED,
                waitingState = null,
                ownEpisodes =
                    listOf(
                        previewEpisode(
                            id = "own-1",
                            placeName = "Rynok Square",
                            description = "Wandered the old town until the streetlights came on.",
                            photoCount = 3,
                        ),
                    ),
                partnerEpisodes =
                    listOf(
                        previewEpisode(
                            id = "partner-1",
                            placeName = "Vinnytsia Fountain",
                            description = "The evening fountain show, filmed badly on a phone.",
                            photoCount = 2,
                        ),
                    ),
                ownClosedAt = null,
            ),
        ),
    )
}

private fun previewDayCard(
    ownEpisodes: List<Episode> = emptyList(),
    ownClosedAt: Instant? = null,
) = DiaryDayCard(
    date = LocalDate(2026, 7, 19),
    dayIndex = 3,
    unlockState = DayUnlockState.LOCKED,
    waitingState = DiaryDayWaitingState.PARTNER_CAPTURING,
    ownEpisodes = ownEpisodes,
    partnerEpisodes = emptyList(),
    ownClosedAt = ownClosedAt,
)

@Preview
@Composable
private fun TimelineWithAddPhotosButtonPreview() {
    AlongsideTheme {
        DiaryTimelineContent(
            items = listOf(DiaryTimelineItem.Day(previewDayCard())),
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Preview
@Composable
private fun TimelineWithUnclosedEntryShowsCloseDayButtonPreview() {
    AlongsideTheme {
        DiaryTimelineContent(
            items =
                listOf(
                    DiaryTimelineItem.Day(
                        previewDayCard(
                            ownEpisodes =
                                listOf(
                                    previewEpisode(
                                        id = "own-1",
                                        placeName = "Rynok Square",
                                        description = "Wandered the old town.",
                                        photoCount = 1,
                                    ),
                                ),
                        ),
                    ),
                ),
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Preview
@Composable
private fun TimelineWithClosedEntryShowsClosedLabelPreview() {
    AlongsideTheme {
        DiaryTimelineContent(
            items =
                listOf(
                    DiaryTimelineItem.Day(
                        previewDayCard(
                            ownEpisodes =
                                listOf(
                                    previewEpisode(
                                        id = "own-1",
                                        placeName = "Rynok Square",
                                        description = "Wandered the old town.",
                                        photoCount = 1,
                                    ),
                                ),
                            ownClosedAt = Instant.fromEpochMilliseconds(1),
                        ),
                    ),
                ),
            modifier = Modifier.fillMaxSize(),
        )
    }
}
