package com.alongside.playground

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.alongside.core.model.SyncStatus
import com.alongside.core.model.place.PlaceCandidate
import com.alongside.core.model.recap.CategorySwipeTally
import com.alongside.core.model.recap.RecapSlide
import com.alongside.core.ui.component.MediaListRow
import com.alongside.core.ui.component.PaperCard
import com.alongside.core.ui.theme.AlongsideSpacing
import com.alongside.core.ui.theme.alongsideColors
import com.alongside.core.ui.theme.alongsideTypography
import kotlin.time.Instant

private val OwnPhotoBrush = Brush.linearGradient(listOf(Color(0xFFE2764A), Color(0xFF8A3A1F)))
private val PartnerPhotoBrush = Brush.linearGradient(listOf(Color(0xFF4A7CE2), Color(0xFF1F3A8A)))
private val DayPhotoBrush = Brush.linearGradient(listOf(Color(0xFF4AE28A), Color(0xFF1F8A4A)))

private fun playgroundPlaceCandidate(
    id: String,
    name: String,
    category: String? = null,
) = PlaceCandidate(
    id = id,
    tripId = "trip-1",
    name = name,
    latitude = 0.0,
    longitude = 0.0,
    note = null,
    addedByUserId = "own",
    syncStatus = SyncStatus.SYNCED,
    createdAt = Instant.fromEpochMilliseconds(0),
    updatedAt = Instant.fromEpochMilliseconds(0),
    category = category,
)

private val PlaygroundIntro = RecapSlide.Intro(cities = listOf("Rzeszów", "Lviv", "Ternopil", "Vinnytsia", "Kyiv"))

private val PlaygroundSwipeArchetype =
    RecapSlide.SwipeArchetype(
        ownTally =
            listOf(
                CategorySwipeTally("Coffee", likeCount = 5, dislikeCount = 1),
                CategorySwipeTally("Museum", likeCount = 1, dislikeCount = 3),
            ),
        partnerTally =
            listOf(
                CategorySwipeTally("Coffee", likeCount = 4, dislikeCount = 2),
                CategorySwipeTally("Museum", likeCount = 4, dislikeCount = 0),
            ),
    )

private val PlaygroundUnresolvedQuestion =
    RecapSlide.UnresolvedQuestion(
        candidates = listOf(playgroundPlaceCandidate("p1", "Rynok Square", "Landmark")),
    )

private val PlaygroundMatchList =
    RecapSlide.MatchList(
        candidates =
            listOf(
                playgroundPlaceCandidate("m1", "Lviv Coffee Manufacture", "Coffee shop"),
                playgroundPlaceCandidate("m2", "Roshen Fountain", "Landmark"),
            ),
    )

private val PlaygroundFinal = RecapSlide.Final(daysTogether = 7)

@Composable
private fun StoryFrame(content: @Composable () -> Unit) {
    Box(
        modifier =
            Modifier
                .size(320.dp, 620.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.alongsideColors.gradientTop),
        content = { content() },
    )
}

@Composable
private fun SlideCaption(
    headline: String,
    caption: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(AlongsideSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(AlongsideSpacing.sm),
    ) {
        Text(text = headline, style = MaterialTheme.typography.headlineSmall, color = Color.White)
        caption?.let {
            Text(text = it, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f))
        }
    }
}

@Composable
private fun IntroSlideSection() {
    Section(title = "Recap — Intro") {
        StoryFrame {
            Column(
                modifier = Modifier.fillMaxSize().padding(AlongsideSpacing.xxl),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Your trip,\ntogether",
                    style = MaterialTheme.alongsideTypography.displaySerifItalic,
                    color = Color.White,
                )
                Column(
                    modifier = Modifier.padding(top = AlongsideSpacing.xxl),
                    verticalArrangement = Arrangement.spacedBy(AlongsideSpacing.sm),
                ) {
                    PlaygroundIntro.cities.forEach { city ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(7.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                            Text(
                                text = city,
                                modifier = Modifier.padding(start = AlongsideSpacing.sm),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.85f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TwoPhotoSlideSection(
    title: String,
    headline: String,
    caption: String,
) {
    Section(title = title) {
        StoryFrame {
            Box(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f).fillMaxHeight().background(OwnPhotoBrush))
                    Box(Modifier.width(2.dp).fillMaxHeight().background(Color.Black))
                    Box(Modifier.weight(1f).fillMaxHeight().background(PartnerPhotoBrush))
                }
                SlideCaption(headline = headline, caption = caption, modifier = Modifier.align(Alignment.BottomCenter))
            }
        }
    }
}

@Composable
private fun DayHighlightSlideSection() {
    Section(title = "Recap — Day highlight") {
        StoryFrame {
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize().background(DayPhotoBrush))
                SlideCaption(
                    headline = "Day 3 · Ternopil",
                    caption = "\"Lake at sunset, then the best cherry dumplings of the whole trip.\"",
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}

@Composable
private fun SwipeArchetypeSlideSection() {
    Section(title = "Recap — Swipe archetype") {
        StoryFrame {
            Column(
                modifier = Modifier.fillMaxSize().padding(AlongsideSpacing.xl),
                verticalArrangement = Arrangement.spacedBy(AlongsideSpacing.lg),
            ) {
                Text(
                    text = "Your taste",
                    style = MaterialTheme.alongsideTypography.displaySerifItalic,
                    color = Color.White,
                )
                PlaygroundSwipeArchetype.ownTally.forEach { own ->
                    val partner = PlaygroundSwipeArchetype.partnerTally.find { it.category == own.category }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(own.category, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                        val ownCount = "${own.likeCount}/${own.dislikeCount}"
                        val partnerCount = "${partner?.likeCount ?: 0}/${partner?.dislikeCount ?: 0}"
                        Text(
                            "you $ownCount · them $partnerCount",
                            style = MaterialTheme.alongsideTypography.meta,
                            color = Color.White.copy(alpha = 0.7f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaceListSlideSection(
    title: String,
    headline: String,
    candidates: List<PlaceCandidate>,
) {
    Section(title = title) {
        StoryFrame {
            Column(
                modifier = Modifier.fillMaxSize().padding(AlongsideSpacing.xl),
                verticalArrangement = Arrangement.spacedBy(AlongsideSpacing.md),
            ) {
                Text(text = headline, style = MaterialTheme.alongsideTypography.displaySerifItalic, color = Color.White)
                candidates.forEach { candidate ->
                    PaperCard {
                        MediaListRow(
                            imageModels = listOf(null),
                            imageContentDescription = candidate.name,
                            title = candidate.name,
                            subtitle = candidate.category,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FinalSlideSection() {
    Section(title = "Recap — Final") {
        StoryFrame {
            Column(
                modifier = Modifier.fillMaxSize().padding(AlongsideSpacing.xxl),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.alongsideColors.gradientBottom),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = PlaygroundFinal.daysTogether.toString(),
                        style = MaterialTheme.alongsideTypography.digit,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    text = "Days together.",
                    modifier = Modifier.padding(top = AlongsideSpacing.lg),
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                )
                Text(
                    text = "Home again, already counting down to the next one.",
                    modifier = Modifier.padding(top = AlongsideSpacing.xs),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                )
                Row(modifier = Modifier.padding(top = AlongsideSpacing.lg)) {
                    Box(Modifier.size(26.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                    Box(
                        Modifier
                            .offset(x = (-8).dp)
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.9f)),
                    )
                }
            }
        }
    }
}

/**
 * Recap slide-content prototyping (docs/roadmap.md M20.3.5): one Story-frame section per
 * `RecapSlide` variant, colored gradients standing in for real photos (same approach as M12.9's
 * `PhotoGallerySection`) so layout/typography can be tuned live via hot reload before porting the
 * final version - with real `AsyncPhotoBanner`/`MediaListRow` photo loading - into `core:ui`.
 * `MediaListRow` itself is already a finished `core:ui` component, so `UnresolvedQuestion`/
 * `MatchList` use it directly here rather than a colored-box stand-in.
 */
@Composable
internal fun RecapSlidesSection() {
    Section(title = "Recap slides") {
        Column(verticalArrangement = Arrangement.spacedBy(AlongsideSpacing.xxl)) {
            IntroSlideSection()
            TwoPhotoSlideSection(
                title = "Recap — Parallel lives",
                headline = "Parallel lives",
                caption = "482 km apart, before this trip",
            )
            DayHighlightSlideSection()
            TwoPhotoSlideSection(
                title = "Recap — Closest moment",
                headline = "Closest moment",
                caption = "40 m apart, Jul 22",
            )
            SwipeArchetypeSlideSection()
            PlaceListSlideSection(
                title = "Recap — Unresolved question",
                headline = "Still deciding",
                candidates = PlaygroundUnresolvedQuestion.candidates,
            )
            PlaceListSlideSection(
                title = "Recap — Match list",
                headline = "Your matches",
                candidates = PlaygroundMatchList.candidates,
            )
            FinalSlideSection()
        }
    }
}
