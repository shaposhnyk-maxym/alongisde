package com.alongside.core.ui.recap

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alongside.core.model.recap.CategorySwipeTally
import com.alongside.core.model.recap.RecapSlide
import com.alongside.core.ui.component.InkGradientBackground
import com.alongside.core.ui.theme.AlongsideSpacing
import com.alongside.core.ui.theme.AlongsideTheme
import com.alongside.core.ui.theme.alongsideTypography

/**
 * Like/dislike taste comparison by category (docs/roadmap.md M20.3.5) - not mocked in the design
 * source (added to `RecapSlide` after that mockup was built), so this lays out own/partner tallies
 * side by side per category, following the deck's shared ink-canvas/typography rather than any
 * specific reference screen.
 */
@Composable
public fun SwipeArchetypeSlideContent(
    slide: RecapSlide.SwipeArchetype,
    modifier: Modifier = Modifier,
) {
    InkGradientBackground(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(AlongsideSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(AlongsideSpacing.lg),
        ) {
            Text(text = "Your taste", style = MaterialTheme.alongsideTypography.displaySerifItalic)
            slide.ownTally.forEach { own ->
                val partner = slide.partnerTally.find { it.category == own.category }
                CategoryTallyRow(own, partner)
            }
        }
    }
}

@Composable
private fun CategoryTallyRow(
    own: CategorySwipeTally,
    partner: CategorySwipeTally?,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = own.category, style = MaterialTheme.typography.bodyMedium)
        val ownCount = "${own.likeCount}/${own.dislikeCount}"
        val partnerCount = "${partner?.likeCount ?: 0}/${partner?.dislikeCount ?: 0}"
        Text(
            text = "you $ownCount · them $partnerCount",
            style = MaterialTheme.alongsideTypography.meta,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview
@Composable
private fun SwipeArchetypeSlideContentPreview() {
    AlongsideTheme {
        Box(Modifier.size(360.dp, 780.dp)) {
            SwipeArchetypeSlideContent(
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
                ),
            )
        }
    }
}
