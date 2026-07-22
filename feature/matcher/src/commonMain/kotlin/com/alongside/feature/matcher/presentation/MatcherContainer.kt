package com.alongside.feature.matcher.presentation

import androidx.lifecycle.ViewModel
import com.alongside.core.domain.auth.AuthSessionCache
import com.alongside.core.domain.pairing.PairingRepository
import com.alongside.core.domain.place.PlaceCandidateRepository
import com.alongside.core.domain.place.PlaceContentPuller
import com.alongside.core.domain.place.PlaceSwipeRepository
import com.alongside.core.model.SyncStatus
import com.alongside.core.model.place.PlaceCandidate
import com.alongside.core.model.place.PlaceSwipe
import com.alongside.core.model.place.SwipeDirection
import com.alongside.core.model.trip.Trip
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.container
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

private data class TripContent(
    val trip: Trip?,
    val candidates: List<PlaceCandidate>,
    val swipes: List<PlaceSwipe>,
)

private val TRIP_CONTENT_POLL_INTERVAL = 5.seconds

public class MatcherContainer(
    private val placeCandidateRepository: PlaceCandidateRepository,
    private val placeSwipeRepository: PlaceSwipeRepository,
    private val pairingRepository: PairingRepository,
    private val authSessionCache: AuthSessionCache,
    private val placeContentPuller: PlaceContentPuller,
    private val clock: Clock = Clock.System,
) : ViewModel(),
    ContainerHost<MatcherState, MatcherSideEffect> {
    override val container: Container<MatcherState, MatcherSideEffect> =
        container(MatcherState()) { observeCandidatesAndSwipes() }

    public fun onIntent(intent: MatcherIntent) {
        when (intent) {
            is MatcherIntent.Swipe -> swipe(intent.candidateId, intent.direction)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun Syntax<MatcherState, MatcherSideEffect>.observeCandidatesAndSwipes() {
        val uid = authSessionCache.get()?.user?.uid ?: return
        reduce { state.copy(ownUserId = uid) }

        val tripFlow = pairingRepository.observeActiveTrip(uid)

        coroutineScope {
            // A second, independent collection of tripFlow - accepted duplicate polling of the
            // underlying pairing poll loop in exchange for clean cancellation: distinctUntilChanged
            // + collectLatest means exactly one trip-content poll loop runs at a time, automatically
            // replaced (not stacked) when the active trip changes, and stopped when it goes null.
            // The periodic WorkManager sweep (PlaceContentPullCoordinator) is the backstop for when
            // this screen isn't open.
            launch {
                tripFlow
                    .map { it?.id }
                    .distinctUntilChanged()
                    .collectLatest { tripId -> if (tripId != null) pollTripContent(tripId, uid) }
            }

            // A fresh MatcherContainer (e.g. re-navigating to this tab, which tears down and
            // recreates the ViewModel - see AlongsideApp's tab-switch wiring) starts from an
            // empty MatcherState. Without this flag, its very first content load would report
            // EVERY already-matched candidate already sitting in Room as "newly matched" and
            // re-fire the Matched banner for old news. Only the first emission is a baseline -
            // everything after it is a real diff against the previous (already-reduced) state.
            var hasLoadedInitialContent = false
            tripFlow
                .flatMapLatest { trip -> observeTripContent(trip) }
                .collect { content ->
                    val previousMatchIds = state.matches.map { it.id }.toSet()
                    reduce {
                        state.copy(trip = content.trip, candidates = content.candidates, swipes = content.swipes)
                    }
                    if (hasLoadedInitialContent) {
                        state.matches
                            .filter { it.id !in previousMatchIds }
                            .forEach { postSideEffect(MatcherSideEffect.Matched(it)) }
                    }
                    hasLoadedInitialContent = true
                }
        }
    }

    private suspend fun pollTripContent(
        tripId: String,
        ownUserId: String,
    ) {
        while (true) {
            runCatching { placeContentPuller.pullTripContent(tripId, ownUserId) }
                .onFailure { println("MatcherContainer: pullTripContent failed: $it") }
            delay(TRIP_CONTENT_POLL_INTERVAL)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeTripContent(trip: Trip?): Flow<TripContent> {
        if (trip == null) return flowOf(TripContent(trip = null, candidates = emptyList(), swipes = emptyList()))
        return combine(
            placeCandidateRepository.observeByTrip(trip.id),
            placeSwipeRepository.observeByTrip(trip.id),
        ) { candidates, swipes -> TripContent(trip, candidates, swipes) }
    }

    private fun swipe(
        candidateId: String,
        direction: SwipeDirection,
    ) = intent {
        val uid = state.ownUserId ?: return@intent
        val tripId = state.trip?.id ?: return@intent
        val now = clock.now()
        placeSwipeRepository.upsert(
            PlaceSwipe(
                id = "$candidateId::$uid",
                tripId = tripId,
                candidateId = candidateId,
                userId = uid,
                direction = direction,
                swipedAt = now,
                syncStatus = SyncStatus.PENDING,
                updatedAt = now,
            ),
        )
    }
}
