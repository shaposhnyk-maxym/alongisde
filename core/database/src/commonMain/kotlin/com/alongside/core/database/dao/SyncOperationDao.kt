package com.alongside.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.alongside.core.database.entity.SyncOperationEntity

@Dao
internal interface SyncOperationDao {
    @Insert
    suspend fun insert(operation: SyncOperationEntity)

    @Query("SELECT * FROM sync_operations ORDER BY seq")
    suspend fun getAll(): List<SyncOperationEntity>

    @Query("DELETE FROM sync_operations WHERE opId IN (:opIds)")
    suspend fun deleteByOpIds(opIds: List<String>)

    @Query("UPDATE sync_operations SET status = :status, attempts = :attempts WHERE opId = :opId")
    suspend fun updateStatus(
        opId: String,
        status: String,
        attempts: Int,
    )
}
