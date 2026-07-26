package com.alongside.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.alongside.core.database.entity.RecapEntity
import kotlinx.coroutines.flow.Flow

@Dao
internal interface RecapDao {
    // IGNORE, not the codebase-standard REPLACE - "set once" insert (accept criterion 4:
    // idempotent, must not overwrite an existing row's availableAt), not an overwriting upsert.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun ensureScheduled(recap: RecapEntity)

    @Query("SELECT * FROM recaps WHERE tripId = :tripId")
    suspend fun getById(tripId: String): RecapEntity?

    @Query("SELECT * FROM recaps WHERE tripId = :tripId")
    fun observeById(tripId: String): Flow<RecapEntity?>
}
