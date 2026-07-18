package com.alongside.core.database.repository

import com.alongside.core.database.AlongsideDatabase
import com.alongside.core.database.entity.toDomain
import com.alongside.core.database.entity.toEntity
import com.alongside.core.domain.pairing.PairingTripDataSource
import com.alongside.core.model.trip.Trip
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class RoomPairingTripDataSource(
    database: AlongsideDatabase,
) : PairingTripDataSource {
    private val dao = database.tripDao()

    override suspend fun findByInviteCode(code: String): Trip? = dao.getByInviteCode(code)?.toDomain()

    override fun observeByUserId(userId: String): Flow<Trip?> = dao.observeByUserId(userId).map { it?.toDomain() }

    override suspend fun save(trip: Trip) {
        dao.upsert(trip.toEntity())
    }
}
