package com.alongside.feature.matcher.presentation

import com.alongside.core.model.place.SwipeDirection
import com.alongside.feature.matcher.FakeAuthSessionCache
import com.alongside.feature.matcher.FakePairingRepository
import com.alongside.feature.matcher.FakePlaceCandidateRepository
import com.alongside.feature.matcher.FakePlaceContentPuller
import com.alongside.feature.matcher.FakePlaceSwipeRepository
import com.alongside.feature.matcher.fakeCandidate
import com.alongside.feature.matcher.fakeSwipe
import com.alongside.feature.matcher.fakeTrip
import com.alongside.feature.matcher.testAuthSession
import kotlinx.coroutines.test.runTest
import org.orbitmvi.orbit.test.test
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Instant

private val FIXED_NOW = Instant.fromEpochMilliseconds(1_752_800_000_000)

private object FixedClock : Clock {
    override fun now(): Instant = FIXED_NOW
}

class MatcherContainerTest {
    private val placeCandidateRepository = FakePlaceCandidateRepository()
    private val placeSwipeRepository = FakePlaceSwipeRepository()
    private val pairingRepository = FakePairingRepository()

    private fun containerUnderTest(uid: String = "owner-1") =
        MatcherContainer(
            placeCandidateRepository = placeCandidateRepository,
            placeSwipeRepository = placeSwipeRepository,
            pairingRepository = pairingRepository,
            authSessionCache = FakeAuthSessionCache(testAuthSession(uid)),
            placeContentPuller = FakePlaceContentPuller(),
            clock = FixedClock,
        )

    @Test
    fun `with no active trip the deck stays empty`() =
        runTest {
            containerUnderTest().test(this) {
                runOnCreate()
                val state = awaitState()
                assertEquals("owner-1", state.ownUserId)
                assertEquals(emptyList(), state.deck)
                cancelAndIgnoreRemainingItems()
            }
        }

    @Test
    fun `deck shows every candidate neither side has fully decided on`() =
        runTest {
            pairingRepository.activeTrip.value = fakeTrip()
            placeCandidateRepository.seed(fakeCandidate("place-1"), fakeCandidate("place-2"))

            containerUnderTest().test(this) {
                runOnCreate()
                awaitState() // ownUserId bootstrap
                val loaded = awaitState() // trip + candidates loaded

                assertEquals(listOf("place-1", "place-2"), loaded.deck.map { it.id })
                assertEquals(emptyList(), loaded.matches)

                cancelAndIgnoreRemainingItems()
            }
        }

    @Test
    fun `swiping a candidate the partner hasn't seen yet keeps it pending in the deck`() =
        runTest {
            pairingRepository.activeTrip.value = fakeTrip()
            placeCandidateRepository.seed(fakeCandidate("place-1"))

            containerUnderTest().test(this) {
                runOnCreate()
                awaitState()
                awaitState()

                containerHost.onIntent(MatcherIntent.Swipe("place-1", SwipeDirection.LIKE))
                val afterSwipe = awaitState()

                assertEquals(listOf("place-1"), afterSwipe.deck.map { it.id })
                assertEquals(emptyList(), afterSwipe.matches)

                cancelAndIgnoreRemainingItems()
            }
        }

    @Test
    fun `swiping LIKE when the partner already liked matches immediately and fires Matched`() =
        runTest {
            pairingRepository.activeTrip.value = fakeTrip()
            placeCandidateRepository.seed(fakeCandidate("place-1"))
            placeSwipeRepository.seed(fakeSwipe("place-1", "member-1", SwipeDirection.LIKE))

            containerUnderTest(uid = "owner-1").test(this) {
                runOnCreate()
                awaitState()
                awaitState()

                containerHost.onIntent(MatcherIntent.Swipe("place-1", SwipeDirection.LIKE))
                val matched = awaitState()

                assertEquals(emptyList(), matched.deck)
                assertEquals(listOf("place-1"), matched.matches.map { it.id })
                expectSideEffect(MatcherSideEffect.Matched(matched.matches.single()))

                cancelAndIgnoreRemainingItems()
            }
        }

    @Test
    fun `swiping DISLIKE when the partner already liked stays pending and the card remains in the deck`() =
        runTest {
            pairingRepository.activeTrip.value = fakeTrip()
            placeCandidateRepository.seed(fakeCandidate("place-1"))
            placeSwipeRepository.seed(fakeSwipe("place-1", "member-1", SwipeDirection.LIKE))

            containerUnderTest(uid = "owner-1").test(this) {
                runOnCreate()
                awaitState()
                awaitState()

                containerHost.onIntent(MatcherIntent.Swipe("place-1", SwipeDirection.DISLIKE))
                val afterSplit = awaitState()

                assertEquals(listOf("place-1"), afterSplit.deck.map { it.id })
                assertEquals(emptyList(), afterSplit.matches)

                cancelAndIgnoreRemainingItems()
            }
        }

    @Test
    fun `both sides disliking removes the card from the deck for good`() =
        runTest {
            pairingRepository.activeTrip.value = fakeTrip()
            placeCandidateRepository.seed(fakeCandidate("place-1"))
            placeSwipeRepository.seed(fakeSwipe("place-1", "member-1", SwipeDirection.DISLIKE))

            containerUnderTest(uid = "owner-1").test(this) {
                runOnCreate()
                awaitState()
                awaitState()

                containerHost.onIntent(MatcherIntent.Swipe("place-1", SwipeDirection.DISLIKE))
                val rejected = awaitState()

                assertEquals(emptyList(), rejected.deck)
                assertEquals(emptyList(), rejected.matches)

                cancelAndIgnoreRemainingItems()
            }
        }

    @Test
    fun `disagreeing then reconsidering to agree eventually moves the card into matches`() =
        runTest {
            pairingRepository.activeTrip.value = fakeTrip()
            placeCandidateRepository.seed(fakeCandidate("place-1"))
            placeSwipeRepository.seed(fakeSwipe("place-1", "member-1", SwipeDirection.LIKE))

            containerUnderTest(uid = "owner-1").test(this) {
                runOnCreate()
                awaitState()
                awaitState()

                containerHost.onIntent(MatcherIntent.Swipe("place-1", SwipeDirection.DISLIKE))
                val split = awaitState()
                assertEquals(listOf("place-1"), split.deck.map { it.id })

                containerHost.onIntent(MatcherIntent.Swipe("place-1", SwipeDirection.LIKE))
                val reconsidered = awaitState()
                assertEquals(listOf("place-1"), reconsidered.matches.map { it.id })
                expectSideEffect(MatcherSideEffect.Matched(reconsidered.matches.single()))

                cancelAndIgnoreRemainingItems()
            }
        }

    @Test
    fun `deck is empty once every candidate is either matched or rejected`() =
        runTest {
            pairingRepository.activeTrip.value = fakeTrip()
            placeCandidateRepository.seed(fakeCandidate("place-1"), fakeCandidate("place-2"))
            placeSwipeRepository.seed(
                fakeSwipe("place-1", "member-1", SwipeDirection.LIKE),
                fakeSwipe("place-1", "owner-1", SwipeDirection.LIKE),
                fakeSwipe("place-2", "member-1", SwipeDirection.DISLIKE),
                fakeSwipe("place-2", "owner-1", SwipeDirection.DISLIKE),
            )

            containerUnderTest(uid = "owner-1").test(this) {
                runOnCreate()
                awaitState()
                val loaded = awaitState()

                assertEquals(emptyList(), loaded.deck)
                assertEquals(listOf("place-1"), loaded.matches.map { it.id })

                cancelAndIgnoreRemainingItems()
            }
        }

    @Test
    fun `swiping as the owner writes a swipe record keyed by the owner's userId`() =
        runTest {
            pairingRepository.activeTrip.value = fakeTrip(ownerId = "owner-1", memberId = "member-1")
            placeCandidateRepository.seed(fakeCandidate("place-1"))

            containerUnderTest(uid = "owner-1").test(this) {
                runOnCreate()
                awaitState()
                awaitState()
                containerHost.onIntent(MatcherIntent.Swipe("place-1", SwipeDirection.LIKE))
                awaitState()
                cancelAndIgnoreRemainingItems()
            }

            val recorded = placeSwipeRepository.upserted.single()
            assertEquals("owner-1", recorded.userId)
            assertEquals("place-1::owner-1", recorded.id)
        }

    @Test
    fun `swiping as the member writes a swipe record keyed by the member's userId`() =
        runTest {
            pairingRepository.activeTrip.value = fakeTrip(ownerId = "owner-1", memberId = "member-1")
            placeCandidateRepository.seed(fakeCandidate("place-1"))

            containerUnderTest(uid = "member-1").test(this) {
                runOnCreate()
                awaitState()
                awaitState()
                containerHost.onIntent(MatcherIntent.Swipe("place-1", SwipeDirection.LIKE))
                awaitState()
                cancelAndIgnoreRemainingItems()
            }

            val recorded = placeSwipeRepository.upserted.single()
            assertEquals("member-1", recorded.userId)
            assertEquals("place-1::member-1", recorded.id)
        }

    @Test
    fun `our own swipe and a partner's swipe on the same card resolve to a consistent match regardless of order`() =
        runTest {
            pairingRepository.activeTrip.value = fakeTrip()
            placeCandidateRepository.seed(fakeCandidate("place-1"))

            containerUnderTest(uid = "owner-1").test(this) {
                runOnCreate()
                awaitState()
                awaitState()

                containerHost.onIntent(MatcherIntent.Swipe("place-1", SwipeDirection.LIKE))
                awaitState()

                placeSwipeRepository.seed(fakeSwipe("place-1", "member-1", SwipeDirection.LIKE))
                val finalState = awaitState()

                assertEquals(listOf("place-1"), finalState.matches.map { it.id })
                assertEquals(emptyList(), finalState.deck)

                cancelAndIgnoreRemainingItems()
            }
        }
}
