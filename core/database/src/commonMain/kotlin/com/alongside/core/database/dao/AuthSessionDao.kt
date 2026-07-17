package com.alongside.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.alongside.core.database.entity.AuthSessionEntity

@Dao
internal interface AuthSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: AuthSessionEntity)

    @Query("SELECT * FROM auth_session WHERE id = 'current'")
    suspend fun get(): AuthSessionEntity?

    @Query("DELETE FROM auth_session WHERE id = 'current'")
    suspend fun clear()
}
