package com.alongside.app.home

import com.alongside.core.domain.auth.AuthSessionCache
import com.alongside.core.domain.pairing.JoinTripResult
import com.alongside.core.domain.pairing.PairingRepository
import com.alongside.core.domain.recap.RecapRepository
import com.alongside.core.model.SyncStatus
import com.alongside.core.model.auth.AuthSession
import com.alongside.core.model.auth.AuthUser
import com.alongside.core.model.recap.Recap
import com.alongside.core.model.trip.Trip
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

internal fun testAuthSession(uid: String = "uid-1"): AuthSession =
    AuthSession(
        user = AuthUser(uid = uid, email = null, displayName = null, photoUrl = null),
        idToken = "id-token",
        refreshToken = null,
        expiresInSeconds = 3600L,
        issuedAt = Instant.fromEpochMilliseconds(0),
    )

internal class FakeAuthSessionCache(
    private var session: AuthSession? = testAuthSession(),
) : AuthSessionCache {
    override suspend fun get(): AuthSession? = session

    override suspend fun save(session: AuthSession) {
        this.session = session
    }

    override suspend fun clear() {
        session = null
    }
}

internal fun fakeTrip(
    id: String = "trip-1",
    ownerId: String = "uid-1",
    memberId: String? = "partner-1",
    inviteCode: String = "ABCD23",
    startDate: LocalDate = LocalDate(2026, 7, 18),
    endDate: LocalDate = LocalDate(2026, 8, 1),
): Trip =
    Trip(
        id = id,
        ownerId = ownerId,
        memberId = memberId,
        inviteCode = inviteCode,
        startDate = startDate,
        endDate = endDate,
        syncStatus = SyncStatus.SYNCED,
        createdAt = Instant.fromEpochMilliseconds(0),
        updatedAt = Instant.fromEpochMilliseconds(0),
    )

/** Only [observeActiveTrip] is exercised by Home - create/join belong to feature:pairing. */
internal class FakePairingRepository : PairingRepository {
    val activeTrip = MutableStateFlow<Trip?>(null)

    override suspend fun createTrip(
        ownerId: String,
        startDate: LocalDate,
        endDate: LocalDate,
    ): Trip = error("not used by Home")

    override suspend fun joinTrip(
        code: String,
        userId: String,
    ): JoinTripResult = error("not used by Home")

    override fun observeActiveTrip(userId: String): Flow<Trip?> = activeTrip

    override suspend fun getActiveTrip(userId: String): Trip? = activeTrip.value
}

internal class FakeRecapRepository : RecapRepository {
    private val recaps = MutableStateFlow<Map<String, Recap>>(emptyMap())

    override suspend fun ensureScheduled(
        tripId: String,
        availableAt: LocalDate,
    ) {
        recaps.value = recaps.value + (tripId to Recap(tripId = tripId, availableAt = availableAt))
    }

    override suspend fun getById(tripId: String): Recap? = recaps.value[tripId]

    override fun observeById(tripId: String): Flow<Recap?> = recaps.map { it[tripId] }
}
