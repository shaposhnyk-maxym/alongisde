package com.alongside.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.alongside.core.database.entity.TripEntity
import kotlinx.coroutines.flow.Flow

@Dao
internal interface TripDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(trip: TripEntity)

    @Query("SELECT * FROM trips WHERE id = :id")
    suspend fun getById(id: String): TripEntity?

    @Query("SELECT * FROM trips WHERE id = :id")
    fun observeById(id: String): Flow<TripEntity?>

    @Query("SELECT * FROM trips WHERE inviteCode = :code LIMIT 1")
    suspend fun getByInviteCode(code: String): TripEntity?

    // ORDER BY, not a bare LIMIT 1: old trips are never purged after they end, so a user can
    // have multiple matching rows (e.g. a completed prior trip kept for Recap history). Without
    // ordering, SQLite's tie-break is undefined and can return a stale, unrelated row instead of
    // the user's actual current trip.
    @Query("SELECT * FROM trips WHERE ownerId = :userId OR memberId = :userId ORDER BY updatedAt DESC LIMIT 1")
    fun observeByUserId(userId: String): Flow<TripEntity?>

    @Query("SELECT * FROM trips WHERE ownerId = :userId OR memberId = :userId ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getByUserId(userId: String): TripEntity?

    @Query("DELETE FROM trips WHERE id = :id")
    suspend fun delete(id: String)
}
