package com.alongside.data.pairing

import com.alongside.core.model.trip.Trip
import com.alongside.core.network.firestore.FirestoreException

/** Scripted remote pairing lookups with a blanket-outage switch. */
internal class FakePairingRemoteDataSource : PairingRemoteDataSource {
    val tripsByInviteCode = mutableMapOf<String, Trip>()
    val tripsByUserId = mutableMapOf<String, Trip>()
    var unreachable: Boolean = false
    var inviteCodeLookups: Int = 0
    var userIdLookups: Int = 0

    override suspend fun findTripByInviteCode(code: String): Trip? {
        inviteCodeLookups++
        throwIfUnreachable()
        return tripsByInviteCode[code]
    }

    override suspend fun findTripByUserId(userId: String): Trip? {
        userIdLookups++
        throwIfUnreachable()
        return tripsByUserId[userId]
    }

    private fun throwIfUnreachable() {
        if (unreachable) {
            throw FirestoreException.ServerError(code = 503, status = "UNAVAILABLE", message = "scripted outage")
        }
    }
}
