package com.alongside.core.database.converter

import androidx.room.TypeConverter
import com.alongside.core.model.SyncStatus
import com.alongside.core.model.place.SwipeDirection
import com.alongside.core.model.push.PushPlatform
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

internal object AlongsideTypeConverters {
    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? = date?.toString()

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let { LocalDate.parse(it) }

    @TypeConverter
    fun fromInstant(instant: Instant?): Long? = instant?.toEpochMilliseconds()

    @TypeConverter
    fun toInstant(value: Long?): Instant? = value?.let { Instant.fromEpochMilliseconds(it) }

    @TypeConverter
    fun fromSyncStatus(status: SyncStatus?): String? = status?.name

    @TypeConverter
    fun toSyncStatus(value: String?): SyncStatus? = value?.let { SyncStatus.valueOf(it) }

    @TypeConverter
    fun fromSwipeDirection(direction: SwipeDirection?): String? = direction?.name

    @TypeConverter
    fun toSwipeDirection(value: String?): SwipeDirection? = value?.let { SwipeDirection.valueOf(it) }

    @TypeConverter
    fun fromPushPlatform(platform: PushPlatform?): String? = platform?.name

    @TypeConverter
    fun toPushPlatform(value: String?): PushPlatform? = value?.let { PushPlatform.valueOf(it) }
}
