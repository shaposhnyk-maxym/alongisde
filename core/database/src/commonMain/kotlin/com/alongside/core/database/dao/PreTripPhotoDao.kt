package com.alongside.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.alongside.core.database.entity.PreTripPhotoEntity
import kotlinx.coroutines.flow.Flow

@Dao
internal interface PreTripPhotoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(photo: PreTripPhotoEntity)

    @Query("SELECT * FROM pre_trip_photos WHERE id = :id")
    suspend fun getById(id: String): PreTripPhotoEntity?

    @Query("SELECT * FROM pre_trip_photos WHERE tripId = :tripId AND userId = :userId")
    fun observeByTripAndUser(
        tripId: String,
        userId: String,
    ): Flow<List<PreTripPhotoEntity>>

    @Query("DELETE FROM pre_trip_photos WHERE id = :id")
    suspend fun delete(id: String)
}
