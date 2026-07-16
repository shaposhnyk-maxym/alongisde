package com.alongside.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.alongside.core.database.entity.PlaceCandidateEntity
import kotlinx.coroutines.flow.Flow

@Dao
internal interface PlaceCandidateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(place: PlaceCandidateEntity)

    @Query("SELECT * FROM place_candidates WHERE id = :id")
    suspend fun getById(id: String): PlaceCandidateEntity?

    @Query("SELECT * FROM place_candidates WHERE tripId = :tripId")
    fun observeByTrip(tripId: String): Flow<List<PlaceCandidateEntity>>

    @Query("DELETE FROM place_candidates WHERE id = :id")
    suspend fun delete(id: String)
}
