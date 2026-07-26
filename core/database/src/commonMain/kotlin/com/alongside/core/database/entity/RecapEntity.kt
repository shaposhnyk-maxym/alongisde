package com.alongside.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.alongside.core.model.recap.Recap
import kotlinx.datetime.LocalDate

@Entity(tableName = "recaps")
internal data class RecapEntity(
    @PrimaryKey val tripId: String,
    val availableAt: LocalDate,
)

internal fun RecapEntity.toDomain(): Recap = Recap(tripId = tripId, availableAt = availableAt)

internal fun Recap.toEntity(): RecapEntity = RecapEntity(tripId = tripId, availableAt = availableAt)
