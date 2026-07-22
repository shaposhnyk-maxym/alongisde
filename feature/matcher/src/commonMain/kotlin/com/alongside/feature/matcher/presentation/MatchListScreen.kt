package com.alongside.feature.matcher.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.alongside.core.model.place.PlaceCandidate
import com.alongside.core.ui.component.InkBackground
import com.alongside.core.ui.component.MediaListRow
import com.alongside.core.ui.component.OverlineLabel
import com.alongside.core.ui.component.OverlineLabelTone
import com.alongside.core.ui.component.PaperCard
import com.alongside.core.ui.format.countryCodeToFlagEmoji
import com.alongside.core.ui.theme.AlongsideSpacing
import org.orbitmvi.orbit.compose.collectAsState
import kotlin.math.round

private val MatchBadgeSize = 26.dp
private val MatchBadgeIconSize = 14.dp
private const val OTHER_CITY_LABEL = "Other"

@Composable
public fun MatchListScreen(
    container: MatcherContainer,
    modifier: Modifier = Modifier,
) {
    val state by container.collectAsState()
    MatchListContent(state = state, modifier = modifier)
}

@Composable
internal fun MatchListContent(
    state: MatcherState,
    modifier: Modifier = Modifier,
) {
    InkBackground(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(AlongsideSpacing.xl)) {
            Text(text = "Our Matches", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(AlongsideSpacing.lg))
            if (state.matches.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "No matches yet - swipe on some places together.")
                }
            } else {
                MatchesByCity(state.matches.groupedByCity())
            }
        }
    }
}

@Composable
private fun MatchesByCity(groups: List<PlaceCityGroup>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(AlongsideSpacing.md),
    ) {
        groups.forEach { group ->
            item(key = "header-${group.city ?: OTHER_CITY_LABEL}") {
                val flag = group.countryCode?.let { " ${countryCodeToFlagEmoji(it)}" }.orEmpty()
                OverlineLabel(
                    text = "${(group.city ?: OTHER_CITY_LABEL).uppercase()}$flag",
                    tone = OverlineLabelTone.Muted,
                )
            }
            items(group.places, key = { it.id }) { place -> MatchRow(place) }
        }
    }
}

@Composable
private fun MatchRow(place: PlaceCandidate) {
    PaperCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MediaListRow(
                modifier = Modifier.weight(1f),
                imageModels = place.photos.mapNotNull { it.remoteUrl },
                imageContentDescription = place.name,
                title = place.name,
                titleAccent = place.rating?.let(::formatRating),
                subtitle = place.category,
            )
            Box(
                modifier =
                    Modifier
                        .size(MatchBadgeSize)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(MatchBadgeIconSize),
                )
            }
        }
    }
}

/** KMP-safe one-decimal rounding - no `String.format`/`java.util.Formatter` in commonMain. */
private fun formatRating(rating: Double): String = (round(rating * 10) / 10).toString()
