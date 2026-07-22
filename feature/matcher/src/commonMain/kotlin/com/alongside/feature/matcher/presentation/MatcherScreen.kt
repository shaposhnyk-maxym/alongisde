package com.alongside.feature.matcher.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import com.alongside.core.model.place.PlaceCandidate
import com.alongside.core.model.place.SwipeDirection
import com.alongside.core.ui.component.CircleIconButton
import com.alongside.core.ui.component.CircleIconButtonStyle
import com.alongside.core.ui.component.DotBanner
import com.alongside.core.ui.component.InkBackground
import com.alongside.core.ui.theme.AlongsideSpacing
import com.alongside.core.ui.theme.alongsideTypography
import kotlinx.coroutines.delay
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

private const val MATCH_BANNER_DURATION_MILLIS = 3000L
private const val CARD_STACK_FRACTION = 0.94f

@Composable
public fun MatcherScreen(
    container: MatcherContainer,
    modifier: Modifier = Modifier,
) {
    val state by container.collectAsState()
    var matchedPlaceName by remember { mutableStateOf<String?>(null) }

    container.collectSideEffect { effect ->
        when (effect) {
            is MatcherSideEffect.Matched -> matchedPlaceName = effect.place.name
        }
    }

    LaunchedEffect(matchedPlaceName) {
        if (matchedPlaceName != null) {
            delay(MATCH_BANNER_DURATION_MILLIS)
            matchedPlaceName = null
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        MatcherContent(
            state = state,
            onSwipe = { candidateId, direction -> container.onIntent(MatcherIntent.Swipe(candidateId, direction)) },
            modifier = Modifier.fillMaxSize(),
        )
        matchedPlaceName?.let { name ->
            DotBanner(
                text = "It's a match: $name!",
                modifier = Modifier.align(Alignment.TopCenter).padding(AlongsideSpacing.lg),
            )
        }
    }
}

/**
 * Pure state-driven content (no container), for previews/screenshot/UI tests. Renders a local
 * queue reconciled against [MatcherState.myTurnDeck] rather than that list directly: a naive
 * `myTurnDeck.firstOrNull()` would show the same card again the instant a swipe creates or
 * revisits a split (both sides answered, disagreeing) - swiped candidates are removed from the
 * front immediately, and only rejoin the queue - at the *end* - once the container confirms
 * they're still my turn.
 */
@Composable
internal fun MatcherContent(
    state: MatcherState,
    onSwipe: (String, SwipeDirection) -> Unit,
    modifier: Modifier = Modifier,
) {
    val queue = remember { mutableStateListOf<String>() }
    val candidatesById = remember(state.candidates) { state.candidates.associateBy { it.id } }

    LaunchedEffect(state.myTurnDeck) {
        val myTurnIds = state.myTurnDeck.map { it.id }.toSet()
        queue.retainAll(myTurnIds)
        state.myTurnDeck
            .map { it.id }
            .filterNot { it in queue }
            .forEach { queue.add(it) }
    }

    val top = queue.firstOrNull()?.let(candidatesById::get)

    fun decide(
        candidate: PlaceCandidate,
        direction: SwipeDirection,
    ) {
        queue.removeAt(0)
        onSwipe(candidate.id, direction)
    }

    InkBackground(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(AlongsideSpacing.xl)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Matcher", style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = "${queue.size} left",
                    style = MaterialTheme.alongsideTypography.meta,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(AlongsideSpacing.lg))
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                if (top != null) {
                    Surface(
                        modifier = Modifier.fillMaxSize(CARD_STACK_FRACTION).rotate(3f),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {}
                    Surface(
                        modifier = Modifier.fillMaxSize(CARD_STACK_FRACTION).rotate(-2f),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {}
                    key(top.id) {
                        SwipeableCard(
                            candidate = top,
                            onSwipe = { direction -> decide(top, direction) },
                            modifier = Modifier.fillMaxSize(CARD_STACK_FRACTION),
                        )
                    }
                } else {
                    Text(
                        text = "No places left to decide on right now.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            if (top != null) {
                Spacer(modifier = Modifier.height(AlongsideSpacing.lg))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AlongsideSpacing.xxl, Alignment.CenterHorizontally),
                ) {
                    CircleIconButton(
                        onClick = { decide(top, SwipeDirection.DISLIKE) },
                        contentDescription = "Skip ${top.name}",
                        style = CircleIconButtonStyle.Dark,
                    ) {
                        Text("✕")
                    }
                    CircleIconButton(
                        onClick = { decide(top, SwipeDirection.LIKE) },
                        contentDescription = "Want to go to ${top.name}",
                        style = CircleIconButtonStyle.Primary,
                    ) {
                        Text("♥")
                    }
                }
            }
        }
    }
}
