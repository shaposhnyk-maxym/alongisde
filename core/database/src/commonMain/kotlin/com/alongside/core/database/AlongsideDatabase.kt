package com.alongside.core.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.alongside.core.database.converter.AlongsideTypeConverters
import com.alongside.core.database.converter.PlacePhotoListTypeConverters
import com.alongside.core.database.converter.StringListTypeConverters
import com.alongside.core.database.dao.AuthSessionDao
import com.alongside.core.database.dao.DiaryEntryDao
import com.alongside.core.database.dao.EpisodeDao
import com.alongside.core.database.dao.PlaceCandidateDao
import com.alongside.core.database.dao.PushTokenDao
import com.alongside.core.database.dao.SyncOperationDao
import com.alongside.core.database.dao.TripDao
import com.alongside.core.database.entity.AuthSessionEntity
import com.alongside.core.database.entity.DiaryEntryEntity
import com.alongside.core.database.entity.EpisodeEntity
import com.alongside.core.database.entity.PhotoEntity
import com.alongside.core.database.entity.PlaceCandidateEntity
import com.alongside.core.database.entity.PushTokenEntity
import com.alongside.core.database.entity.SyncOperationEntity
import com.alongside.core.database.entity.TripEntity
import com.alongside.core.database.migration.MIGRATION_10_11
import com.alongside.core.database.migration.MIGRATION_11_12
import com.alongside.core.database.migration.MIGRATION_12_13
import com.alongside.core.database.migration.MIGRATION_3_4
import com.alongside.core.database.migration.MIGRATION_4_5
import com.alongside.core.database.migration.MIGRATION_5_6
import com.alongside.core.database.migration.MIGRATION_6_7
import com.alongside.core.database.migration.MIGRATION_7_8
import com.alongside.core.database.migration.MIGRATION_8_9
import com.alongside.core.database.migration.MIGRATION_9_10
import com.alongside.core.database.repository.AuthSessionCacheImpl
import com.alongside.core.database.repository.DiaryEntryRepositoryImpl
import com.alongside.core.database.repository.EpisodeRepositoryImpl
import com.alongside.core.database.repository.PlaceCandidateRepositoryImpl
import com.alongside.core.database.repository.RoomPairingTripDataSource
import com.alongside.core.database.repository.SyncOperationStoreImpl
import com.alongside.core.database.repository.TripRepositoryImpl
import com.alongside.core.database.sync.SyncOperationStore
import com.alongside.core.domain.auth.AuthSessionCache
import com.alongside.core.domain.diary.DiaryEntryRepository
import com.alongside.core.domain.diary.EpisodeRepository
import com.alongside.core.domain.pairing.PairingTripDataSource
import com.alongside.core.domain.place.PlaceCandidateRepository
import com.alongside.core.domain.trip.TripRepository
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
        SyncOperationEntity::class,
    ],
    version = 13,
    exportSchema = true,
)
@TypeConverters(AlongsideTypeConverters::class, StringListTypeConverters::class, PlacePhotoListTypeConverters::class)
@ConstructedBy(AlongsideDatabaseConstructor::class)
public abstract class AlongsideDatabase : RoomDatabase() {
    internal abstract fun tripDao(): TripDao

    internal abstract fun diaryEntryDao(): DiaryEntryDao

    internal abstract fun placeCandidateDao(): PlaceCandidateDao

    internal abstract fun episodeDao(): EpisodeDao

    internal abstract fun pushTokenDao(): PushTokenDao

    internal abstract fun authSessionDao(): AuthSessionDao

    internal abstract fun syncOperationDao(): SyncOperationDao
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
        .addMigrations(
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6,
            MIGRATION_6_7,
            MIGRATION_7_8,
            MIGRATION_8_9,
            MIGRATION_9_10,
            MIGRATION_10_11,
            MIGRATION_11_12,
            MIGRATION_12_13,
        ).build()

/** Factory rather than a public [AuthSessionCacheImpl] - keeps the Room-backed impl an internal detail. */
public fun AlongsideDatabase.authSessionCache(): AuthSessionCache = AuthSessionCacheImpl(this)

/** Room-backed local [TripRepository] - the `data` module wraps it with sync-queue enqueueing. */
public fun AlongsideDatabase.tripRepository(): TripRepository = TripRepositoryImpl(this)

/** Room-backed local [DiaryEntryRepository] - the `data` module wraps it with sync-queue enqueueing. */
public fun AlongsideDatabase.diaryEntryRepository(): DiaryEntryRepository = DiaryEntryRepositoryImpl(this)

/** Room-backed local [EpisodeRepository] - the `data` module wraps it with sync-queue enqueueing. */
public fun AlongsideDatabase.episodeRepository(): EpisodeRepository = EpisodeRepositoryImpl(this)

/** Room-backed local [PlaceCandidateRepository] - the `data` module wraps it with sync-queue enqueueing. */
public fun AlongsideDatabase.placeCandidateRepository(): PlaceCandidateRepository = PlaceCandidateRepositoryImpl(this)

/** Local (Room) side of pairing lookups - the `data` module composes it with the Firestore side. */
public fun AlongsideDatabase.pairingTripLocalDataSource(): PairingTripDataSource = RoomPairingTripDataSource(this)

public fun AlongsideDatabase.syncOperationStore(): SyncOperationStore = SyncOperationStoreImpl(this)
