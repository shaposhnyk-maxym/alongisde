package com.alongside.core.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.alongside.core.database.converter.AlongsideTypeConverters
import com.alongside.core.database.dao.AuthSessionDao
import com.alongside.core.database.dao.DiaryEntryDao
import com.alongside.core.database.dao.EpisodeDao
import com.alongside.core.database.dao.PlaceCandidateDao
import com.alongside.core.database.dao.PushTokenDao
import com.alongside.core.database.dao.TripDao
import com.alongside.core.database.entity.AuthSessionEntity
import com.alongside.core.database.entity.DiaryEntryEntity
import com.alongside.core.database.entity.EpisodeEntity
import com.alongside.core.database.entity.PhotoEntity
import com.alongside.core.database.entity.PlaceCandidateEntity
import com.alongside.core.database.entity.PushTokenEntity
import com.alongside.core.database.entity.TripEntity
import com.alongside.core.database.repository.AuthSessionCacheImpl
import com.alongside.core.domain.auth.AuthSessionCache
import kotlinx.coroutines.Dispatchers

internal const val DATABASE_FILE_NAME = "alongside.db"

@Database(
    entities = [
        TripEntity::class,
        DiaryEntryEntity::class,
        PlaceCandidateEntity::class,
        EpisodeEntity::class,
        PhotoEntity::class,
        PushTokenEntity::class,
        AuthSessionEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
@TypeConverters(AlongsideTypeConverters::class)
@ConstructedBy(AlongsideDatabaseConstructor::class)
public abstract class AlongsideDatabase : RoomDatabase() {
    internal abstract fun tripDao(): TripDao

    internal abstract fun diaryEntryDao(): DiaryEntryDao

    internal abstract fun placeCandidateDao(): PlaceCandidateDao

    internal abstract fun episodeDao(): EpisodeDao

    internal abstract fun pushTokenDao(): PushTokenDao

    internal abstract fun authSessionDao(): AuthSessionDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
internal expect object AlongsideDatabaseConstructor : RoomDatabaseConstructor<AlongsideDatabase> {
    override fun initialize(): AlongsideDatabase
}

// Dispatchers.IO is internal on Kotlin/Native — Dispatchers.Default is the
// one query-coroutine dispatcher choice available across every target this
// module builds for (android/jvm/iosArm64/iosSimulatorArm64).
public fun getRoomDatabase(builder: RoomDatabase.Builder<AlongsideDatabase>): AlongsideDatabase =
    builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.Default)
        .build()

/** Factory rather than a public [AuthSessionCacheImpl] - keeps the Room-backed impl an internal detail. */
public fun AlongsideDatabase.authSessionCache(): AuthSessionCache = AuthSessionCacheImpl(this)
