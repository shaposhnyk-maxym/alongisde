package com.alongside.core.domain.pairing

import kotlin.test.Test
import kotlin.test.assertEquals

class ResolveJoinOutcomeTest {
    @Test
    fun `valid code and free trip joins with member set`() {
        val trip = pairingTestTrip()
        assertEquals(
            JoinTripResult.Joined(trip.copy(memberId = "user-2")),
            resolveJoinOutcome(code = "ABCD23", userId = "user-2", candidate = trip),
        )
    }

    @Test
    fun `malformed code is invalid even when a candidate is passed`() {
        assertEquals(
            JoinTripResult.InvalidCode,
            resolveJoinOutcome(code = "abc", userId = "user-2", candidate = pairingTestTrip()),
        )
    }

    @Test
    fun `unknown code is invalid`() {
        assertEquals(
            JoinTripResult.InvalidCode,
            resolveJoinOutcome(code = "ABCD23", userId = "user-2", candidate = null),
        )
    }

    @Test
    fun `own code is rejected for the owner`() {
        assertEquals(
            JoinTripResult.OwnCode,
            resolveJoinOutcome(code = "ABCD23", userId = "owner-1", candidate = pairingTestTrip()),
        )
    }

    @Test
    fun `code of a trip with another member is already used`() {
        assertEquals(
            JoinTripResult.AlreadyUsed,
            resolveJoinOutcome(
                code = "ABCD23",
                userId = "user-2",
                candidate = pairingTestTrip(memberId = "someone-else"),
            ),
        )
    }

    @Test
    fun `re-join by the existing member is joined and idempotent`() {
        val trip = pairingTestTrip(memberId = "user-2")
        assertEquals(
            JoinTripResult.Joined(trip),
            resolveJoinOutcome(code = "ABCD23", userId = "user-2", candidate = trip),
        )
    }
}
