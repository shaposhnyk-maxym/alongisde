package com.alongside.core.domain.place

import com.alongside.core.model.place.MatchStatus
import com.alongside.core.model.place.SwipeDirection
import kotlin.test.Test
import kotlin.test.assertEquals

class PlaceMatchResolverTest {
    @Test
    fun `both null is pending`() {
        assertEquals(
            MatchStatus.PENDING,
            resolveMatchStatus(ownerSwipe = null, memberSwipe = null),
        )
    }

    @Test
    fun `only owner swiped is pending`() {
        assertEquals(
            MatchStatus.PENDING,
            resolveMatchStatus(ownerSwipe = SwipeDirection.LIKE, memberSwipe = null),
        )
    }

    @Test
    fun `only member swiped is pending`() {
        assertEquals(
            MatchStatus.PENDING,
            resolveMatchStatus(ownerSwipe = null, memberSwipe = SwipeDirection.DISLIKE),
        )
    }

    @Test
    fun `both like is matched`() {
        assertEquals(
            MatchStatus.MATCHED,
            resolveMatchStatus(ownerSwipe = SwipeDirection.LIKE, memberSwipe = SwipeDirection.LIKE),
        )
    }

    @Test
    fun `both dislike is rejected`() {
        assertEquals(
            MatchStatus.REJECTED,
            resolveMatchStatus(ownerSwipe = SwipeDirection.DISLIKE, memberSwipe = SwipeDirection.DISLIKE),
        )
    }

    @Test
    fun `owner like member dislike is pending`() {
        assertEquals(
            MatchStatus.PENDING,
            resolveMatchStatus(ownerSwipe = SwipeDirection.LIKE, memberSwipe = SwipeDirection.DISLIKE),
        )
    }

    @Test
    fun `owner dislike member like is pending`() {
        assertEquals(
            MatchStatus.PENDING,
            resolveMatchStatus(ownerSwipe = SwipeDirection.DISLIKE, memberSwipe = SwipeDirection.LIKE),
        )
    }

    @Test
    fun `neither side has swiped is my turn`() {
        assertEquals(true, isMyTurn(mine = null, theirs = null))
    }

    @Test
    fun `partner already swiped and I have not is my turn`() {
        assertEquals(true, isMyTurn(mine = null, theirs = SwipeDirection.LIKE))
    }

    @Test
    fun `I already swiped and partner has not is not my turn`() {
        assertEquals(false, isMyTurn(mine = SwipeDirection.LIKE, theirs = null))
    }

    @Test
    fun `both swiped and disagree is my turn to reconsider`() {
        assertEquals(true, isMyTurn(mine = SwipeDirection.DISLIKE, theirs = SwipeDirection.LIKE))
    }

    @Test
    fun `both swiped like is not my turn`() {
        assertEquals(false, isMyTurn(mine = SwipeDirection.LIKE, theirs = SwipeDirection.LIKE))
    }

    @Test
    fun `both swiped dislike is not my turn`() {
        assertEquals(false, isMyTurn(mine = SwipeDirection.DISLIKE, theirs = SwipeDirection.DISLIKE))
    }
}
