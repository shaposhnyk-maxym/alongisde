package com.alongside.feature.places.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.alongside.core.model.place.PlaceCandidate
import com.alongside.core.ui.component.AlongsidePrimaryButton
import com.alongside.core.ui.component.AlongsideTextButton
import com.alongside.core.ui.component.AsyncPhotoTile
import com.alongside.core.ui.component.DotBanner
import com.alongside.core.ui.component.InkGradientBackground
import com.alongside.core.ui.component.OverlineLabel
import com.alongside.core.ui.component.PaperCard
import com.alongside.core.ui.theme.AlongsideSpacing
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@Composable
public fun PlaceImportScreen(
    container: PlaceImportContainer,
    onImport: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by container.collectAsState()

    container.collectSideEffect { effect ->
        when (effect) {
            PlaceImportSideEffect.Imported -> onImport()
            PlaceImportSideEffect.Discarded -> onDiscard()
        }
    }

    PlaceImportContent(
        state = state,
        onAccept = { container.onIntent(PlaceImportIntent.Accept) },
        onDiscard = { container.onIntent(PlaceImportIntent.Discard) },
        modifier = modifier,
    )
}

@Composable
internal fun PlaceImportContent(
    state: PlaceImportState,
    onAccept: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    InkGradientBackground(modifier = modifier.fillMaxSize()) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(AlongsideSpacing.xl)
                    .semantics { contentDescription = "place-import-${state.status.name}" },
            contentAlignment = Alignment.Center,
        ) {
            when (state.status) {
                PlaceImportStatus.LOADING -> CircularProgressIndicator()
                PlaceImportStatus.FOUND ->
                    state.place?.let { place -> FoundCard(place = place, onAccept = onAccept, onDiscard = onDiscard) }
                PlaceImportStatus.NOT_FOUND ->
                    MessageCard(text = "Couldn't find this place on Google Maps.", onDismiss = onDiscard)
                PlaceImportStatus.ERROR ->
                    MessageCard(
                        text = state.errorMessage ?: "Something went wrong importing this place.",
                        onDismiss = onDiscard,
                    )
            }
        }
    }
}

@Composable
private fun MessageCard(
    text: String,
    onDismiss: () -> Unit,
) {
    PaperCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(AlongsideSpacing.md)) {
            DotBanner(text = text)
            AlongsideTextButton(text = "Dismiss", onClick = onDismiss)
        }
    }
}

@Composable
private fun FoundCard(
    place: PlaceCandidate,
    onAccept: () -> Unit,
    onDiscard: () -> Unit,
) {
    PaperCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(AlongsideSpacing.md)) {
            place.photos.firstOrNull()?.remoteUrl?.let { url ->
                AsyncPhotoTile(model = url, contentDescription = place.name, size = 160.dp)
            }
            Text(place.name)
            Row(horizontalArrangement = Arrangement.spacedBy(AlongsideSpacing.sm)) {
                place.rating?.let { rating -> Text("★ $rating") }
                place.category?.let { category -> OverlineLabel(text = category) }
            }
            AlongsidePrimaryButton(text = "Add to Places", onClick = onAccept)
            AlongsideTextButton(text = "Discard", onClick = onDiscard)
        }
    }
}
