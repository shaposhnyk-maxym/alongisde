package com.alongside.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.alongside.core.database.entity.PushTokenEntity
import kotlinx.coroutines.flow.Flow

@Dao
internal interface PushTokenDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(pushToken: PushTokenEntity)

    @Query("SELECT * FROM push_tokens WHERE userId = :userId")
    suspend fun getById(userId: String): PushTokenEntity?

    @Query("SELECT * FROM push_tokens WHERE userId = :userId")
    fun observeById(userId: String): Flow<PushTokenEntity?>

    @Query("DELETE FROM push_tokens WHERE userId = :userId")
    suspend fun delete(userId: String)
}
