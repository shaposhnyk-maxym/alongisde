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

    @Query("SELECT * FROM trips WHERE ownerId = :userId OR memberId = :userId LIMIT 1")
    fun observeByUserId(userId: String): Flow<TripEntity?>

    @Query("SELECT * FROM trips WHERE ownerId = :userId OR memberId = :userId LIMIT 1")
    suspend fun getByUserId(userId: String): TripEntity?

    @Query("DELETE FROM trips WHERE id = :id")
    suspend fun delete(id: String)
}
