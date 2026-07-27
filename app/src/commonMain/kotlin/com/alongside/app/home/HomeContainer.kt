package com.alongside.app.home

import androidx.lifecycle.ViewModel
import com.alongside.core.domain.auth.AuthSessionCache
import com.alongside.core.domain.diary.DiaryEntryRepository
import com.alongside.core.domain.diary.EpisodeRepository
import com.alongside.core.domain.pairing.PairingRepository
import com.alongside.core.domain.place.PlaceCandidateRepository
import com.alongside.core.domain.place.PlaceSwipeRepository
import com.alongside.core.domain.recap.RecapRepository
import com.alongside.core.model.diary.DiaryEntry
import com.alongside.core.model.diary.Episode
import com.alongside.core.model.trip.Trip
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.container
import kotlin.time.Clock

/**
 * Aggregates trip-day progress, today's diary entry and recent matches for the Home tab, plus
 * the recap-availability gate `docs/roadmap.md` M20.1 introduced. Deliberately does not reuse
 * `feature:diary`'s `DiaryTimelineDataSource` - that pipeline builds a card per trip day plus a
 * full episodes-by-entry map for the whole trip (and runs its own content-poll loop); Home only
 * ever needs *today*'s slice, so it queries the two repositories directly instead.
 */
public class HomeContainer(
    private val authSessionCache: AuthSessionCache,
    private val pairingRepository: PairingRepository,
    private val recapRepository: RecapRepository,
    private val diaryEntryRepository: DiaryEntryRepository,
    private val episodeRepository: EpisodeRepository,
    private val placeCandidateRepository: PlaceCandidateRepository,
    private val placeSwipeRepository: PlaceSwipeRepository,
    private val clock: Clock = Clock.System,
) : ViewModel(),
    ContainerHost<HomeState, Nothing> {
    override val container: Container<HomeState, Nothing> = container(HomeState()) { observeHome() }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun Syntax<HomeState, Nothing>.observeHome() {
        val uid = authSessionCache.get()?.user?.uid ?: return
        val today = clock.todayIn(TimeZone.currentSystemDefault())

        pairingRepository
            .observeActiveTrip(uid)
            .flatMapLatest { trip ->
                trip?.let { observeTripSnapshot(it, uid, today) } ?: flowOf(HomeState(today = today))
            }.collect { snapshot -> reduce { snapshot } }
    }

    private fun observeTripSnapshot(
        trip: Trip,
        uid: String,
        today: LocalDate,
    ): Flow<HomeState> {
        val partnerUid = if (trip.ownerId == uid) trip.memberId else trip.ownerId
        val todayEntriesFlow = observeTodayEntries(trip, uid, partnerUid, today)
        val recapFlow = recapRepository.observeById(trip.id)
        val candidatesFlow = placeCandidateRepository.observeByTrip(trip.id)
        val swipesFlow = placeSwipeRepository.observeByTrip(trip.id)

        return combine(todayEntriesFlow, recapFlow, candidatesFlow, swipesFlow) { entries, recap, candidates, swipes ->
            HomeState(
                today = today,
                trip = trip,
                ownTodayEntry = entries.own,
                partnerTodayEntry = entries.partner,
                ownTodayEpisodes = entries.ownEpisodes,
                partnerTodayEpisodes = entries.partnerEpisodes,
                placeCandidates = candidates,
                placeSwipes = swipes,
                isRecapAvailable = recap != null && today >= recap.availableAt,
            )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeTodayEntries(
        trip: Trip,
        uid: String,
        partnerUid: String?,
        today: LocalDate,
    ): Flow<TodayEntries> =
        diaryEntryRepository.observeByTrip(trip.id).flatMapLatest { entries ->
            val own = entries.find { it.userId == uid && it.date == today }
            val partner = entries.find { partnerUid != null && it.userId == partnerUid && it.date == today }
            combine(
                own?.let { episodeRepository.observeByDiaryEntry(it.id) } ?: flowOf(emptyList()),
                partner?.let { episodeRepository.observeByDiaryEntry(it.id) } ?: flowOf(emptyList()),
            ) { ownEpisodes, partnerEpisodes ->
                TodayEntries(own, partner, ownEpisodes, partnerEpisodes)
            }
        }

    private data class TodayEntries(
        val own: DiaryEntry?,
        val partner: DiaryEntry?,
        val ownEpisodes: List<Episode>,
        val partnerEpisodes: List<Episode>,
    )
}
