package com.alongside.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.alongside.core.database.entity.DiaryEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
internal interface DiaryEntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: DiaryEntryEntity)

    @Query("SELECT * FROM diary_entries WHERE id = :id")
    suspend fun getById(id: String): DiaryEntryEntity?

    @Query("SELECT * FROM diary_entries WHERE tripId = :tripId")
    fun observeByTrip(tripId: String): Flow<List<DiaryEntryEntity>>

    @Query("DELETE FROM diary_entries WHERE id = :id")
    suspend fun delete(id: String)
}
