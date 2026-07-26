package com.alongside.feature.recap.presentation

import androidx.lifecycle.ViewModel
import com.alongside.core.domain.auth.AuthSessionCache
import com.alongside.core.domain.diary.DiaryEntryRepository
import com.alongside.core.domain.diary.EpisodeRepository
import com.alongside.core.domain.pairing.PairingRepository
import com.alongside.core.domain.place.PlaceCandidateRepository
import com.alongside.core.domain.place.PlaceSwipeRepository
import com.alongside.core.domain.pretrip.PreTripPhotoRepository
import com.alongside.core.domain.recap.buildRecapSlides
import kotlinx.coroutines.flow.first
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.container

/**
 * Reads the active trip's current local data and builds the recap deck live, each time this
 * Container is created (docs/roadmap.md M20.3) - not a long-lived subscription like
 * `DiaryTimelineContainer`'s, since a Stories deck shouldn't reshuffle under the user mid-view if
 * a late sync lands while they're watching. One-shot `.first()` per repository flow instead.
 */
public class RecapContainer(
    private val authSessionCache: AuthSessionCache,
    private val pairingRepository: PairingRepository,
    private val diaryEntryRepository: DiaryEntryRepository,
    private val episodeRepository: EpisodeRepository,
    private val preTripPhotoRepository: PreTripPhotoRepository,
    private val placeCandidateRepository: PlaceCandidateRepository,
    private val placeSwipeRepository: PlaceSwipeRepository,
) : ViewModel(),
    ContainerHost<RecapState, Nothing> {
    override val container: Container<RecapState, Nothing> = container(RecapState()) { loadRecap() }

    public fun onIntent(intent: RecapIntent) {
        when (intent) {
            is RecapIntent.ChangeActiveSlide -> changeActiveSlide(intent.index)
        }
    }

    private suspend fun Syntax<RecapState, Nothing>.loadRecap() {
        val uid =
            authSessionCache.get()?.user?.uid ?: run {
                reduce { state.copy(isLoading = false) }
                return
            }
        val trip =
            pairingRepository.getActiveTrip(uid) ?: run {
                reduce { state.copy(isLoading = false) }
                return
            }
        val partnerUid = if (trip.ownerId == uid) trip.memberId else trip.ownerId

        val entries = diaryEntryRepository.observeByTrip(trip.id).first()
        val episodesByDiaryEntryId = entries.associate { it.id to episodeRepository.observeByDiaryEntry(it.id).first() }

        val ownPhotos = preTripPhotoRepository.observeByTripAndUser(trip.id, uid).first()
        val partnerPhotos =
            partnerUid?.let { preTripPhotoRepository.observeByTripAndUser(trip.id, it).first() } ?: emptyList()

        val candidates = placeCandidateRepository.observeByTrip(trip.id).first()
        val swipes = placeSwipeRepository.observeByTrip(trip.id).first()

        val slides =
            buildRecapSlides(
                tripStartDate = trip.startDate,
                tripEndDate = trip.endDate,
                ownDiaryEntries = entries.filter { it.userId == uid },
                partnerDiaryEntries = entries.filter { partnerUid != null && it.userId == partnerUid },
                episodesByDiaryEntryId = episodesByDiaryEntryId,
                ownPreTripPhotos = ownPhotos,
                partnerPreTripPhotos = partnerPhotos,
                placeCandidates = candidates,
                ownPlaceSwipes = swipes.filter { it.userId == uid },
                partnerPlaceSwipes = swipes.filter { partnerUid != null && it.userId == partnerUid },
            )
        reduce { state.copy(isLoading = false, slides = slides) }
    }

    private fun changeActiveSlide(index: Int) =
        intent {
            reduce { state.copy(activeIndex = index) }
        }
}
