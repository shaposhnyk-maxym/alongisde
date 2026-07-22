package com.alongside.feature.matcher.presentation

import androidx.lifecycle.ViewModel
import com.alongside.core.domain.auth.AuthSessionCache
import com.alongside.core.domain.pairing.PairingRepository
import com.alongside.core.domain.place.PlaceCandidateRepository
import com.alongside.core.domain.place.PlaceSwipeRepository
import com.alongside.core.model.SyncStatus
import com.alongside.core.model.place.PlaceCandidate
import com.alongside.core.model.place.PlaceSwipe
import com.alongside.core.model.place.SwipeDirection
import com.alongside.core.model.trip.Trip
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.container
import kotlin.time.Clock

private data class TripContent(
    val trip: Trip?,
    val candidates: List<PlaceCandidate>,
    val swipes: List<PlaceSwipe>,
)

public class MatcherContainer(
    private val placeCandidateRepository: PlaceCandidateRepository,
    private val placeSwipeRepository: PlaceSwipeRepository,
    private val pairingRepository: PairingRepository,
    private val authSessionCache: AuthSessionCache,
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

        pairingRepository
            .observeActiveTrip(uid)
            .flatMapLatest { trip -> observeTripContent(trip) }
            .collect { content ->
                val previousMatchIds = state.matches.map { it.id }.toSet()
                reduce { state.copy(trip = content.trip, candidates = content.candidates, swipes = content.swipes) }
                state.matches
                    .filter { it.id !in previousMatchIds }
                    .forEach { postSideEffect(MatcherSideEffect.Matched(it)) }
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
