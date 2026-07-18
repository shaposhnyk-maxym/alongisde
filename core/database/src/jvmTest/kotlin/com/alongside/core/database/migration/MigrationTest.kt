package com.alongside.core.database.migration

import androidx.room.Room
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.alongside.core.database.AlongsideDatabase
import com.alongside.core.database.entity.SyncOperationEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

/**
 * Hand-rolled v3 fixture instead of room-testing's MigrationTestHelper: the raw SQL below is
 * the exact createSql from schemas/3.json. Opening the migrated file through Room afterwards
 * makes Room validate the migrated schema against the generated v4 expectations - a mismatch
 * fails with "Migration didn't properly handle".
 */
class MigrationTest {
    private lateinit var dbFile: File

    @BeforeTest
    fun setUp() {
        dbFile = File.createTempFile("migration-test", ".db")
        dbFile.delete()
        createVersion3Database()
    }

    @AfterTest
    fun tearDown() {
        dbFile.delete()
    }

    private fun createVersion3Database() {
        val connection = BundledSQLiteDriver().open(dbFile.absolutePath)
        try {
            connection.createVersion3SyncableTables()
            connection.createVersion3AuxiliaryTables()
            connection.insertVersion3Rows()
            connection.execSQL("PRAGMA user_version = 3")
        } finally {
            connection.close()
        }
    }

    private fun SQLiteConnection.createVersion3SyncableTables() {
        execSQL(
            "CREATE TABLE IF NOT EXISTS `trips` (`id` TEXT NOT NULL, `ownerId` TEXT NOT NULL, " +
                "`memberId` TEXT, `inviteCode` TEXT NOT NULL, `startDate` TEXT NOT NULL, " +
                "`endDate` TEXT NOT NULL, `syncStatus` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, " +
                "PRIMARY KEY(`id`))",
        )
        execSQL(
            "CREATE TABLE IF NOT EXISTS `diary_entries` (`id` TEXT NOT NULL, `tripId` TEXT NOT NULL, " +
                "`userId` TEXT NOT NULL, `date` TEXT NOT NULL, `syncStatus` TEXT NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
        )
        execSQL("CREATE INDEX IF NOT EXISTS `index_diary_entries_tripId` ON `diary_entries` (`tripId`)")
        execSQL(
            "CREATE TABLE IF NOT EXISTS `place_candidates` (`id` TEXT NOT NULL, `tripId` TEXT NOT NULL, " +
                "`name` TEXT NOT NULL, `latitude` REAL NOT NULL, `longitude` REAL NOT NULL, `note` TEXT, " +
                "`addedByUserId` TEXT NOT NULL, `ownerSwipe` TEXT, `memberSwipe` TEXT, " +
                "`syncStatus` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
        )
        execSQL("CREATE INDEX IF NOT EXISTS `index_place_candidates_tripId` ON `place_candidates` (`tripId`)")
    }

    private fun SQLiteConnection.createVersion3AuxiliaryTables() {
        execSQL(
            "CREATE TABLE IF NOT EXISTS `episodes` (`id` TEXT NOT NULL, `diaryEntryId` TEXT NOT NULL, " +
                "`startTime` INTEGER NOT NULL, `endTime` INTEGER NOT NULL, `latitude` REAL NOT NULL, " +
                "`longitude` REAL NOT NULL, `placeName` TEXT, `description` TEXT, PRIMARY KEY(`id`))",
        )
        execSQL("CREATE INDEX IF NOT EXISTS `index_episodes_diaryEntryId` ON `episodes` (`diaryEntryId`)")
        execSQL(
            "CREATE TABLE IF NOT EXISTS `photos` (`id` TEXT NOT NULL, `episodeId` TEXT NOT NULL, " +
                "`uri` TEXT NOT NULL, `takenAt` INTEGER NOT NULL, `latitude` REAL NOT NULL, " +
                "`longitude` REAL NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`episodeId`) REFERENCES " +
                "`episodes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        execSQL("CREATE INDEX IF NOT EXISTS `index_photos_episodeId` ON `photos` (`episodeId`)")
        execSQL(
            "CREATE TABLE IF NOT EXISTS `push_tokens` (`userId` TEXT NOT NULL, `token` TEXT NOT NULL, " +
                "`platform` TEXT NOT NULL, `syncStatus` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, " +
                "PRIMARY KEY(`userId`))",
        )
        execSQL(
            "CREATE TABLE IF NOT EXISTS `auth_session` (`id` TEXT NOT NULL, `uid` TEXT NOT NULL, " +
                "`email` TEXT, `displayName` TEXT, `photoUrl` TEXT, `idToken` TEXT NOT NULL, " +
                "`refreshToken` TEXT, `expiresInSeconds` INTEGER NOT NULL, `issuedAt` INTEGER NOT NULL, " +
                "PRIMARY KEY(`id`))",
        )
    }

    private fun SQLiteConnection.insertVersion3Rows() {
        execSQL(
            "INSERT INTO trips (id, ownerId, memberId, inviteCode, startDate, endDate, syncStatus, createdAt) " +
                "VALUES ('trip-1', 'owner-1', NULL, 'ABCD23', '2026-07-16', '2026-07-23', 'SYNCED', 111)",
        )
        execSQL(
            "INSERT INTO diary_entries (id, tripId, userId, date, syncStatus, createdAt) " +
                "VALUES ('entry-1', 'trip-1', 'owner-1', '2026-07-16', 'PENDING', 222)",
        )
        execSQL(
            "INSERT INTO place_candidates (id, tripId, name, latitude, longitude, note, addedByUserId, " +
                "ownerSwipe, memberSwipe, syncStatus, createdAt) " +
                "VALUES ('place-1', 'trip-1', 'Cafe', 49.0, 24.0, NULL, 'owner-1', NULL, NULL, 'PENDING', 333)",
        )
    }

    private fun openMigratedDatabase(): AlongsideDatabase =
        Room
            .databaseBuilder<AlongsideDatabase>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .addMigrations(MIGRATION_3_4)
            .build()

    @Test
    fun `migration backfills updatedAt from createdAt on all three syncable tables`() =
        runTest {
            val database = openMigratedDatabase()
            try {
                val trip = database.tripDao().getById("trip-1")
                assertEquals(Instant.fromEpochMilliseconds(111), trip?.createdAt)
                assertEquals(Instant.fromEpochMilliseconds(111), trip?.updatedAt)

                val entry = database.diaryEntryDao().getById("entry-1")
                assertEquals(Instant.fromEpochMilliseconds(222), entry?.updatedAt)

                val place = database.placeCandidateDao().getById("place-1")
                assertEquals(Instant.fromEpochMilliseconds(333), place?.updatedAt)
            } finally {
                database.close()
            }
        }

    @Test
    fun `migrated database has a usable sync operations table`() =
        runTest {
            val database = openMigratedDatabase()
            try {
                val operation =
                    SyncOperationEntity(
                        opId = "op-1",
                        collectionPath = "trips",
                        documentId = "trip-1",
                        type = "UPSERT",
                        fieldsJson = "{}",
                        attempts = 0,
                        status = "PENDING",
                        enqueuedAt = Instant.fromEpochMilliseconds(444),
                    )

                database.syncOperationDao().insert(operation)

                assertEquals(listOf(operation.copy(seq = 1)), database.syncOperationDao().getAll())
            } finally {
                database.close()
            }
        }
}
