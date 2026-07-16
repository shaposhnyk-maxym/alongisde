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

    @Query("DELETE FROM trips WHERE id = :id")
    suspend fun delete(id: String)
}
