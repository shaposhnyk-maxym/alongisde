package com.alongside.feature.matcher.presentation

import com.alongside.core.model.place.SwipeDirection
import com.alongside.feature.matcher.fakeCandidate
import com.alongside.feature.matcher.fakeSwipe
import com.alongside.feature.matcher.fakeTrip
import kotlin.test.Test
import kotlin.test.assertEquals

class MatcherStateTest {
    private val trip = fakeTrip(ownerId = "owner-1", memberId = "member-1")
    private val candidate = fakeCandidate("place-1")

    @Test
    fun `a fresh candidate with no swipes is in myTurnDeck`() {
        val state = MatcherState(ownUserId = "owner-1", trip = trip, candidates = listOf(candidate))

        assertEquals(listOf("place-1"), state.myTurnDeck.map { it.id })
    }

    @Test
    fun `a candidate I already swiped on while the partner is silent is excluded`() {
        val state =
            MatcherState(
                ownUserId = "owner-1",
                trip = trip,
                candidates = listOf(candidate),
                swipes = listOf(fakeSwipe("place-1", "owner-1", SwipeDirection.LIKE)),
            )

        assertEquals(emptyList(), state.myTurnDeck)
    }

    @Test
    fun `a candidate the partner already swiped on while I have not is in myTurnDeck`() {
        val state =
            MatcherState(
                ownUserId = "owner-1",
                trip = trip,
                candidates = listOf(candidate),
                swipes = listOf(fakeSwipe("place-1", "member-1", SwipeDirection.LIKE)),
            )

        assertEquals(listOf("place-1"), state.myTurnDeck.map { it.id })
    }

    @Test
    fun `a split candidate is in myTurnDeck for reconsideration`() {
        val state =
            MatcherState(
                ownUserId = "owner-1",
                trip = trip,
                candidates = listOf(candidate),
                swipes =
                    listOf(
                        fakeSwipe("place-1", "owner-1", SwipeDirection.DISLIKE),
                        fakeSwipe("place-1", "member-1", SwipeDirection.LIKE),
                    ),
            )

        assertEquals(listOf("place-1"), state.myTurnDeck.map { it.id })
    }

    @Test
    fun `a matched candidate is excluded from myTurnDeck`() {
        val state =
            MatcherState(
                ownUserId = "owner-1",
                trip = trip,
                candidates = listOf(candidate),
                swipes =
                    listOf(
                        fakeSwipe("place-1", "owner-1", SwipeDirection.LIKE),
                        fakeSwipe("place-1", "member-1", SwipeDirection.LIKE),
                    ),
            )

        assertEquals(emptyList(), state.myTurnDeck)
    }

    @Test
    fun `a rejected candidate is excluded from myTurnDeck`() {
        val state =
            MatcherState(
                ownUserId = "owner-1",
                trip = trip,
                candidates = listOf(candidate),
                swipes =
                    listOf(
                        fakeSwipe("place-1", "owner-1", SwipeDirection.DISLIKE),
                        fakeSwipe("place-1", "member-1", SwipeDirection.DISLIKE),
                    ),
            )

        assertEquals(emptyList(), state.myTurnDeck)
    }

    @Test
    fun `myTurnDeck is empty when ownUserId is not yet known`() {
        val state = MatcherState(ownUserId = null, trip = trip, candidates = listOf(candidate))

        assertEquals(emptyList(), state.myTurnDeck)
    }

    @Test
    fun `a candidate I imported myself is excluded from deck even with no swipes`() {
        val ownCandidate = fakeCandidate("place-own", addedByUserId = "owner-1")
        val state = MatcherState(ownUserId = "owner-1", trip = trip, candidates = listOf(ownCandidate))

        assertEquals(emptyList(), state.deck)
        assertEquals(emptyList(), state.myTurnDeck)
    }

    @Test
    fun `a candidate the partner imported still shows in deck alongside a self-imported one`() {
        val ownCandidate = fakeCandidate("place-own", addedByUserId = "owner-1")
        val partnerCandidate = fakeCandidate("place-partner", addedByUserId = "member-1")
        val state =
            MatcherState(ownUserId = "owner-1", trip = trip, candidates = listOf(ownCandidate, partnerCandidate))

        assertEquals(listOf("place-partner"), state.deck.map { it.id })
    }
}
