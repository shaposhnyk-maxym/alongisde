package com.alongside.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.alongside.core.database.entity.OnboardingCompletionEntity

@Dao
internal interface OnboardingCompletionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: OnboardingCompletionEntity)

    @Query("SELECT * FROM onboarding_completion WHERE id = 'current'")
    suspend fun get(): OnboardingCompletionEntity?

    @Query("DELETE FROM onboarding_completion WHERE id = 'current'")
    suspend fun clear()
}
