package com.alongside.feature.places.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.alongside.core.ui.component.InkBackground
import com.alongside.core.ui.component.MediaListRow
import com.alongside.core.ui.component.OverlineLabel
import com.alongside.core.ui.component.OverlineLabelTone
import com.alongside.core.ui.component.PaperCard
import com.alongside.core.ui.component.ScreenHeader
import com.alongside.core.ui.format.countryCodeToFlagEmoji
import com.alongside.core.ui.theme.AlongsideSpacing
import org.orbitmvi.orbit.compose.collectAsState
import kotlin.math.round

private const val OTHER_CITY_LABEL = "Other"

@Composable
public fun PlacesListScreen(
    container: PlacesListContainer,
    modifier: Modifier = Modifier,
) {
    val state by container.collectAsState()

    PlacesListContent(state = state, modifier = modifier)
}

@Composable
internal fun PlacesListContent(
    state: PlacesListState,
    modifier: Modifier = Modifier,
) {
    InkBackground(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(title = "Places", modifier = Modifier.padding(AlongsideSpacing.lg))
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when {
                    state.isLoading ->
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    state.places.isEmpty() ->
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "No places yet - share one from Google Maps to get started.",
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                    else -> PlacesByCity(state.places.groupedByCity())
                }
            }
        }
    }
}

@Composable
private fun PlacesByCity(groups: List<PlaceCityGroup>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(AlongsideSpacing.lg),
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
            items(group.places, key = { it.id }) { place ->
                PaperCard(modifier = Modifier.fillMaxWidth()) {
                    MediaListRow(
                        imageModels = place.photos.mapNotNull { it.remoteUrl },
                        imageContentDescription = place.name,
                        title = place.name,
                        titleAccent = place.rating?.let(::formatRating),
                        subtitle = place.category,
                    )
                }
            }
        }
    }
}

/** KMP-safe one-decimal rounding - no `String.format`/`java.util.Formatter` in commonMain. */
internal fun formatRating(rating: Double): String = (round(rating * 10) / 10).toString()
