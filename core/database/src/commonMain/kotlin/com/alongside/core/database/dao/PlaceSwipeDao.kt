package com.alongside.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.alongside.core.database.entity.PlaceSwipeEntity
import kotlinx.coroutines.flow.Flow

@Dao
internal interface PlaceSwipeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(swipe: PlaceSwipeEntity)

    @Query("SELECT * FROM place_swipes WHERE id = :id")
    suspend fun getById(id: String): PlaceSwipeEntity?

    @Query("SELECT * FROM place_swipes WHERE tripId = :tripId")
    fun observeByTrip(tripId: String): Flow<List<PlaceSwipeEntity>>
}
