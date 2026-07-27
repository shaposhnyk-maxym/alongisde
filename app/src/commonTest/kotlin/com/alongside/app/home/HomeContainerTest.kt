package com.alongside.app.home

import com.alongside.core.model.place.SwipeDirection
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.orbitmvi.orbit.test.test
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

private val TRIP_START_DATE = LocalDate(2026, 7, 18)
private val TRIP_END_DATE = LocalDate(2026, 7, 19)
private val RECAP_AVAILABLE_AT = LocalDate(2026, 7, 20)

private class FixedClock(
    private val today: LocalDate,
) : Clock {
    // epoch-day arithmetic is the simplest deterministic "this LocalDate as an Instant" - no
    // direct helper for it in the kotlinx-datetime API surface already used elsewhere here.
    override fun now(): Instant = Instant.fromEpochMilliseconds(today.toEpochDays() * 86_400_000L)
}

class HomeContainerTest {
    private val authSessionCache = FakeAuthSessionCache(testAuthSession("uid-1"))
    private val pairingRepository = FakePairingRepository()
    private val recapRepository = FakeRecapRepository()
    private val diaryEntryRepository = FakeDiaryEntryRepository()
    private val episodeRepository = FakeEpisodeRepository()
    private val placeCandidateRepository = FakePlaceCandidateRepository()
    private val placeSwipeRepository = FakePlaceSwipeRepository()

    private fun containerUnderTest(today: LocalDate) =
        HomeContainer(
            authSessionCache = authSessionCache,
            pairingRepository = pairingRepository,
            recapRepository = recapRepository,
            diaryEntryRepository = diaryEntryRepository,
            episodeRepository = episodeRepository,
            placeCandidateRepository = placeCandidateRepository,
            placeSwipeRepository = placeSwipeRepository,
            clock = FixedClock(today),
        )

    @Test
    fun `recap is unavailable before its availableAt date`() =
        runTest {
            val trip = fakeTrip(id = "trip-1", endDate = TRIP_END_DATE)
            pairingRepository.activeTrip.value = trip
            recapRepository.ensureScheduled(trip.id, RECAP_AVAILABLE_AT)

            // Seeded true so the settle-to-false reduce produces a detectable state change -
            // it lands on the same false value the real default already holds, and a
            // StateFlow conflates equal values, so relying on the default would never emit.
            containerUnderTest(today = TRIP_END_DATE).test(this, initialState = HomeState(isRecapAvailable = true)) {
                runOnCreate()
                val state = awaitState()
                assertFalse(state.isRecapAvailable)
                cancelAndIgnoreRemainingItems()
            }
        }

    @Test
    fun `recap is available once today reaches its availableAt date`() =
        runTest {
            val trip = fakeTrip(id = "trip-1", endDate = TRIP_END_DATE)
            pairingRepository.activeTrip.value = trip
            recapRepository.ensureScheduled(trip.id, RECAP_AVAILABLE_AT)

            containerUnderTest(today = RECAP_AVAILABLE_AT).test(this) {
                runOnCreate()
                val state = awaitState()
                assertTrue(state.isRecapAvailable)
                cancelAndIgnoreRemainingItems()
            }
        }

    @Test
    fun `no active trip leaves the recap unavailable`() =
        runTest {
            containerUnderTest(today = RECAP_AVAILABLE_AT).test(this, initialState = HomeState(isRecapAvailable = true)) {
                runOnCreate()
                val state = awaitState()
                assertFalse(state.isRecapAvailable)
                cancelAndIgnoreRemainingItems()
            }
        }

    @Test
    fun `an active trip with no recap row yet leaves the recap unavailable without crashing`() =
        runTest {
            val trip = fakeTrip(id = "trip-1", endDate = TRIP_END_DATE)
            pairingRepository.activeTrip.value = trip

            containerUnderTest(today = RECAP_AVAILABLE_AT).test(this, initialState = HomeState(isRecapAvailable = true)) {
                runOnCreate()
                val state = awaitState()
                assertFalse(state.isRecapAvailable)
                cancelAndIgnoreRemainingItems()
            }
        }

    @Test
    fun `no active trip surfaces as NoActiveTrip without crashing`() =
        runTest {
            containerUnderTest(today = RECAP_AVAILABLE_AT).test(this) {
                runOnCreate()
                val state = awaitState()
                assertEquals(HomeTripPhase.NoActiveTrip, state.phase)
                cancelAndIgnoreRemainingItems()
            }
        }

    @Test
    fun `a trip that hasn't started yet is pre-trip with the days until reunion`() =
        runTest {
            val trip = fakeTrip(id = "trip-1", startDate = TRIP_START_DATE, endDate = TRIP_END_DATE)
            pairingRepository.activeTrip.value = trip

            containerUnderTest(today = LocalDate(2026, 7, 10)).test(this) {
                runOnCreate()
                val phase = assertIs<HomeTripPhase.PreTrip>(awaitState().phase)
                assertEquals(8, phase.daysUntilReunion)
                cancelAndIgnoreRemainingItems()
            }
        }

    @Test
    fun `the first day of the trip is active day 1`() =
        runTest {
            val trip = fakeTrip(id = "trip-1", startDate = TRIP_START_DATE, endDate = LocalDate(2026, 7, 24))
            pairingRepository.activeTrip.value = trip

            containerUnderTest(today = TRIP_START_DATE).test(this) {
                runOnCreate()
                val phase = assertIs<HomeTripPhase.Active>(awaitState().phase)
                assertEquals(1, phase.dayIndex)
                assertEquals(7, phase.totalDays)
                cancelAndIgnoreRemainingItems()
            }
        }

    @Test
    fun `the last day of the trip is still active with the full day index`() =
        runTest {
            val trip = fakeTrip(id = "trip-1", startDate = TRIP_START_DATE, endDate = LocalDate(2026, 7, 24))
            pairingRepository.activeTrip.value = trip

            containerUnderTest(today = LocalDate(2026, 7, 24)).test(this) {
                runOnCreate()
                val phase = assertIs<HomeTripPhase.Active>(awaitState().phase)
                assertEquals(7, phase.dayIndex)
                assertEquals(7, phase.totalDays)
                cancelAndIgnoreRemainingItems()
            }
        }

    @Test
    fun `today summary reflects photos from both partners`() =
        runTest {
            val trip = fakeTrip(id = "trip-1", ownerId = "uid-1", memberId = "partner-1", startDate = TRIP_START_DATE)
            pairingRepository.activeTrip.value = trip
            val today = TRIP_START_DATE
            val ownEntry = fakeDiaryEntry(id = "own-entry", userId = "uid-1", date = today, tripId = trip.id)
            val partnerEntry = fakeDiaryEntry(id = "partner-entry", userId = "partner-1", date = today, tripId = trip.id)
            diaryEntryRepository.entries.value = listOf(ownEntry, partnerEntry)
            episodeRepository.episodes.value =
                listOf(
                    fakeEpisode(
                        id = "own-episode",
                        diaryEntryId = ownEntry.id,
                        placeName = "Rynok Square",
                        city = "Lviv",
                        photos = listOf(fakePhoto(id = "own-photo")),
                    ),
                    fakeEpisode(id = "partner-episode", diaryEntryId = partnerEntry.id, photos = listOf(fakePhoto(id = "partner-photo"))),
                )

            containerUnderTest(today = today).test(this) {
                runOnCreate()
                val phase = assertIs<HomeTripPhase.Active>(awaitState().phase)
                assertTrue(phase.today.ownHasPhotos)
                assertTrue(phase.today.partnerHasPhotos)
                assertEquals("Rynok Square", phase.today.placeName)
                assertEquals("Lviv", phase.today.city)
                cancelAndIgnoreRemainingItems()
            }
        }

    @Test
    fun `today summary with no entries has neither partner marked as having photos`() =
        runTest {
            val trip = fakeTrip(id = "trip-1", startDate = TRIP_START_DATE)
            pairingRepository.activeTrip.value = trip

            containerUnderTest(today = TRIP_START_DATE).test(this) {
                runOnCreate()
                val phase = assertIs<HomeTripPhase.Active>(awaitState().phase)
                assertFalse(phase.today.ownHasPhotos)
                assertFalse(phase.today.partnerHasPhotos)
                assertNull(phase.today.placeName)
                cancelAndIgnoreRemainingItems()
            }
        }

    @Test
    fun `a trip past its end date is completed`() =
        runTest {
            val trip = fakeTrip(id = "trip-1", startDate = TRIP_START_DATE, endDate = TRIP_END_DATE)
            pairingRepository.activeTrip.value = trip

            containerUnderTest(today = LocalDate(2026, 7, 25)).test(this) {
                runOnCreate()
                val phase = assertIs<HomeTripPhase.Completed>(awaitState().phase)
                assertEquals(2, phase.daysTogether)
                cancelAndIgnoreRemainingItems()
            }
        }

    @Test
    fun `recent matches only include mutually liked candidates, newest first, capped at four`() =
        runTest {
            val trip = fakeTrip(id = "trip-1", ownerId = "uid-1", memberId = "partner-1", startDate = TRIP_START_DATE)
            pairingRepository.activeTrip.value = trip

            val matchedIds = listOf("c1", "c2", "c3", "c4", "c5")
            val matched =
                matchedIds.mapIndexed { index, id ->
                    fakePlaceCandidate(
                        id = id,
                        tripId = trip.id,
                        updatedAt =
                            Instant.fromEpochMilliseconds(
                                index * 1_000L,
                            ),
                    )
                }
            val onlyOwnerLiked = fakePlaceCandidate(id = "c6", tripId = trip.id)
            val bothRejected = fakePlaceCandidate(id = "c7", tripId = trip.id)
            placeCandidateRepository.candidates.value = matched + listOf(onlyOwnerLiked, bothRejected)
            placeSwipeRepository.swipes.value =
                matchedIds.flatMap { id ->
                    listOf(
                        fakePlaceSwipe(candidateId = id, userId = "uid-1", direction = SwipeDirection.LIKE, tripId = trip.id),
                        fakePlaceSwipe(candidateId = id, userId = "partner-1", direction = SwipeDirection.LIKE, tripId = trip.id),
                    )
                } +
                listOf(
                    fakePlaceSwipe(candidateId = "c6", userId = "uid-1", direction = SwipeDirection.LIKE, tripId = trip.id),
                    fakePlaceSwipe(candidateId = "c7", userId = "uid-1", direction = SwipeDirection.DISLIKE, tripId = trip.id),
                    fakePlaceSwipe(candidateId = "c7", userId = "partner-1", direction = SwipeDirection.DISLIKE, tripId = trip.id),
                )

            containerUnderTest(today = TRIP_START_DATE).test(this) {
                runOnCreate()
                val matches = awaitState().recentMatches
                assertEquals(listOf("c5", "c4", "c3", "c2"), matches.map { it.id })
                cancelAndIgnoreRemainingItems()
            }
        }
}
