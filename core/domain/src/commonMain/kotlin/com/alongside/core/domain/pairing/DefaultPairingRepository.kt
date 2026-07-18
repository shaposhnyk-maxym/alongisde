package com.alongside.core.domain.pairing

import com.alongside.core.model.SyncStatus
import com.alongside.core.model.trip.Trip
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Pairing logic over the [PairingTripDataSource] seam — platform-free, storage-agnostic. */
public class DefaultPairingRepository
    @OptIn(ExperimentalUuidApi::class)
    constructor(
        private val dataSource: PairingTripDataSource,
        private val inviteCodeGenerator: InviteCodeGenerator,
        private val clock: Clock = Clock.System,
        private val generateTripId: () -> String = { Uuid.random().toString() },
    ) : PairingRepository {
        override suspend fun createTrip(
            ownerId: String,
            startDate: LocalDate,
            endDate: LocalDate,
        ): Trip {
            val inviteCode =
                inviteCodeGenerator.generateUnique { code ->
                    dataSource.findByInviteCode(code) != null
                }
            val trip =
                Trip(
                    id = generateTripId(),
                    ownerId = ownerId,
                    memberId = null,
                    inviteCode = inviteCode,
                    startDate = startDate,
                    endDate = endDate,
                    syncStatus = SyncStatus.PENDING,
                    createdAt = clock.now(),
                )
            dataSource.save(trip)
            return trip
        }

        override suspend fun joinTrip(
            code: String,
            userId: String,
        ): JoinTripResult {
            val normalized = code.trim().uppercase()
            val candidate =
                if (isValidInviteCodeFormat(normalized)) {
                    dataSource.findByInviteCode(normalized)
                } else {
                    null
                }
            val outcome = resolveJoinOutcome(normalized, userId, candidate)
            if (outcome is JoinTripResult.Joined) {
                dataSource.save(outcome.trip)
            }
            return outcome
        }

        override fun observeActiveTrip(userId: String): Flow<Trip?> = dataSource.observeByUserId(userId)
    }
