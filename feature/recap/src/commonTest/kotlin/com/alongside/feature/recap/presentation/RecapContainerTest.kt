package com.alongside.feature.recap.presentation

import com.alongside.core.domain.recap.buildRecapSlides
import com.alongside.core.model.place.SwipeDirection
import com.alongside.feature.recap.FakeAuthSessionCache
import com.alongside.feature.recap.FakeDiaryEntryRepository
import com.alongside.feature.recap.FakeEpisodeRepository
import com.alongside.feature.recap.FakePairingRepository
import com.alongside.feature.recap.FakePlaceCandidateRepository
import com.alongside.feature.recap.FakePlaceSwipeRepository
import com.alongside.feature.recap.FakePreTripPhotoRepository
import com.alongside.feature.recap.fakeTrip
import com.alongside.feature.recap.recapDiaryEntry
import com.alongside.feature.recap.recapEpisode
import com.alongside.feature.recap.recapPlaceCandidate
import com.alongside.feature.recap.recapPreTripPhoto
import com.alongside.feature.recap.recapSwipe
import com.alongside.feature.recap.testAuthSession
import kotlinx.coroutines.test.runTest
import org.orbitmvi.orbit.test.test
import kotlin.test.Test

class RecapContainerTest {
    private val pairingRepository = FakePairingRepository()
    private val diaryEntryRepository = FakeDiaryEntryRepository()
    private val episodeRepository = FakeEpisodeRepository()
    private val preTripPhotoRepository = FakePreTripPhotoRepository()
    private val placeCandidateRepository = FakePlaceCandidateRepository()
    private val placeSwipeRepository = FakePlaceSwipeRepository()

    private fun containerUnderTest(uid: String? = "uid-1") =
        RecapContainer(
            authSessionCache = FakeAuthSessionCache(uid?.let { testAuthSession(it) }),
            pairingRepository = pairingRepository,
            diaryEntryRepository = diaryEntryRepository,
            episodeRepository = episodeRepository,
            preTripPhotoRepository = preTripPhotoRepository,
            placeCandidateRepository = placeCandidateRepository,
            placeSwipeRepository = placeSwipeRepository,
        )

    @Test
    fun `loads the deck by threading real repository data through buildRecapSlides`() =
        runTest {
            val trip = fakeTrip()
            pairingRepository.activeTrip.value = trip

            val ownEntry = recapDiaryEntry("own-entry", tripId = trip.id, userId = "uid-1", date = trip.startDate)
            val partnerEntry =
                recapDiaryEntry("partner-entry", tripId = trip.id, userId = "partner-1", date = trip.startDate)
            diaryEntryRepository.upsert(ownEntry)
            diaryEntryRepository.upsert(partnerEntry)

            val ownEpisode = recapEpisode("own-ep", diaryEntryId = ownEntry.id, city = "Lviv")
            episodeRepository.upsert(ownEpisode)

            val ownPhoto = recapPreTripPhoto("own-photo", tripId = trip.id, userId = "uid-1")
            val partnerPhoto = recapPreTripPhoto("partner-photo", tripId = trip.id, userId = "partner-1")
            preTripPhotoRepository.upsert(ownPhoto)
            preTripPhotoRepository.upsert(partnerPhoto)

            val candidate = recapPlaceCandidate("candidate-1", tripId = trip.id, category = "Coffee")
            placeCandidateRepository.upsert(candidate)
            val ownSwipe =
                recapSwipe(
                    "swipe-own",
                    tripId = trip.id,
                    candidateId = candidate.id,
                    userId = "uid-1",
                    direction = SwipeDirection.LIKE,
                )
            val partnerSwipe =
                recapSwipe(
                    "swipe-partner",
                    tripId = trip.id,
                    candidateId = candidate.id,
                    userId = "partner-1",
                    direction = SwipeDirection.LIKE,
                )
            placeSwipeRepository.upsert(ownSwipe)
            placeSwipeRepository.upsert(partnerSwipe)

            val expectedSlides =
                buildRecapSlides(
                    tripStartDate = trip.startDate,
                    tripEndDate = trip.endDate,
                    ownDiaryEntries = listOf(ownEntry),
                    partnerDiaryEntries = listOf(partnerEntry),
                    episodesByDiaryEntryId = mapOf(ownEntry.id to listOf(ownEpisode), partnerEntry.id to emptyList()),
                    ownPreTripPhotos = listOf(ownPhoto),
                    partnerPreTripPhotos = listOf(partnerPhoto),
                    placeCandidates = listOf(candidate),
                    ownPlaceSwipes = listOf(ownSwipe),
                    partnerPlaceSwipes = listOf(partnerSwipe),
                )

            containerUnderTest().test(this) {
                runOnCreate()
                expectState { copy(isLoading = false, slides = expectedSlides) }
                cancelAndIgnoreRemainingItems()
            }
        }

    @Test
    fun `no signed-in user leaves the deck empty without crashing`() =
        runTest {
            containerUnderTest(uid = null).test(this) {
                runOnCreate()
                expectState { copy(isLoading = false, slides = emptyList()) }
                cancelAndIgnoreRemainingItems()
            }
        }

    @Test
    fun `no active trip leaves the deck empty without crashing`() =
        runTest {
            pairingRepository.activeTrip.value = null

            containerUnderTest().test(this) {
                runOnCreate()
                expectState { copy(isLoading = false, slides = emptyList()) }
                cancelAndIgnoreRemainingItems()
            }
        }

    @Test
    fun `a solo trip with no partner yet still loads a minimal deck`() =
        runTest {
            val trip = fakeTrip(memberId = null)
            pairingRepository.activeTrip.value = trip

            val expectedSlides =
                buildRecapSlides(
                    tripStartDate = trip.startDate,
                    tripEndDate = trip.endDate,
                    ownDiaryEntries = emptyList(),
                    partnerDiaryEntries = emptyList(),
                    episodesByDiaryEntryId = emptyMap(),
                    ownPreTripPhotos = emptyList(),
                    partnerPreTripPhotos = emptyList(),
                    placeCandidates = emptyList(),
                    ownPlaceSwipes = emptyList(),
                    partnerPlaceSwipes = emptyList(),
                )

            containerUnderTest().test(this) {
                runOnCreate()
                expectState { copy(isLoading = false, slides = expectedSlides) }
                cancelAndIgnoreRemainingItems()
            }
        }

    @Test
    fun `changing the active slide updates the index`() =
        runTest {
            val trip = fakeTrip(memberId = null)
            pairingRepository.activeTrip.value = trip
            val expectedSlides =
                buildRecapSlides(
                    tripStartDate = trip.startDate,
                    tripEndDate = trip.endDate,
                    ownDiaryEntries = emptyList(),
                    partnerDiaryEntries = emptyList(),
                    episodesByDiaryEntryId = emptyMap(),
                    ownPreTripPhotos = emptyList(),
                    partnerPreTripPhotos = emptyList(),
                    placeCandidates = emptyList(),
                    ownPlaceSwipes = emptyList(),
                    partnerPlaceSwipes = emptyList(),
                )

            containerUnderTest().test(this) {
                runOnCreate()
                expectState { copy(isLoading = false, slides = expectedSlides) }
                containerHost.onIntent(RecapIntent.ChangeActiveSlide(1))
                expectState { copy(activeIndex = 1) }
                cancelAndIgnoreRemainingItems()
            }
        }
}
