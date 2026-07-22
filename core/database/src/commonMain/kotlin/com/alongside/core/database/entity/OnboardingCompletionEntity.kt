package com.alongside.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Single-row table (fixed [id]) - its mere presence means onboarding has been completed once. */
@Entity(tableName = "onboarding_completion")
internal data class OnboardingCompletionEntity(
    @PrimaryKey val id: String = SINGLETON_ID,
) {
    internal companion object {
        internal const val SINGLETON_ID = "current"
    }
}
