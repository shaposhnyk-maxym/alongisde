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
            .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
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

    @Test
    fun `migration 4 to 5 backfills descriptionAttempts to zero on existing episodes`() =
        runTest {
            val v4File = File.createTempFile("migration-test-v4", ".db")
            v4File.delete()
            createVersion4Database(v4File)
            val database =
                Room
                    .databaseBuilder<AlongsideDatabase>(name = v4File.absolutePath)
                    .setDriver(BundledSQLiteDriver())
                    .setQueryCoroutineContext(Dispatchers.IO)
                    .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
                    .build()
            try {
                val episode = database.episodeDao().getById("episode-1")
                assertEquals(0, episode?.episode?.descriptionAttempts)
            } finally {
                database.close()
                v4File.delete()
            }
        }

    @Test
    fun `migration 5 to 6 backfills syncStatus PENDING and updatedAt from endTime on existing episodes`() =
        runTest {
            val v5File = File.createTempFile("migration-test-v5", ".db")
            v5File.delete()
            createVersion5Database(v5File)
            val database =
                Room
                    .databaseBuilder<AlongsideDatabase>(name = v5File.absolutePath)
                    .setDriver(BundledSQLiteDriver())
                    .setQueryCoroutineContext(Dispatchers.IO)
                    .addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
                    .build()
            try {
                val episode = database.episodeDao().getById("episode-1")
                assertEquals("PENDING", episode?.episode?.syncStatus?.name)
                assertEquals(Instant.fromEpochMilliseconds(200), episode?.episode?.updatedAt)
            } finally {
                database.close()
                v5File.delete()
            }
        }

    @Test
    fun `migration 6 to 7 backfills remoteUrl null on existing photos and round-trips a new non-null value`() =
        runTest {
            val v6File = File.createTempFile("migration-test-v6", ".db")
            v6File.delete()
            createVersion6Database(v6File)
            val database =
                Room
                    .databaseBuilder<AlongsideDatabase>(name = v6File.absolutePath)
                    .setDriver(BundledSQLiteDriver())
                    .setQueryCoroutineContext(Dispatchers.IO)
                    .addMigrations(MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
                    .build()
            try {
                val preMigrationEpisode = database.episodeDao().getById("episode-1")
                assertEquals(null, preMigrationEpisode?.photos?.single { it.id == "photo-1" }?.remoteUrl)

                val episode = requireNotNull(preMigrationEpisode).episode
                val originalPhoto = preMigrationEpisode.photos.single { it.id == "photo-1" }
                val photoWithRemoteUrl =
                    originalPhoto.copy(remoteUrl = "https://firebasestorage.googleapis.com/photos%2Fphoto-1")
                database.episodeDao().upsert(episode, listOf(photoWithRemoteUrl))

                val reloaded = database.episodeDao().getById("episode-1")
                val roundTripPhoto = reloaded?.photos?.single { it.id == "photo-1" }
                assertEquals(
                    "https://firebasestorage.googleapis.com/photos%2Fphoto-1",
                    roundTripPhoto?.remoteUrl,
                )
            } finally {
                database.close()
                v6File.delete()
            }
        }

    @Test
    fun `migration 7 to 8 backfills closedAt null on existing diary entries and round-trips a new non-null value`() =
        runTest {
            val v7File = File.createTempFile("migration-test-v7", ".db")
            v7File.delete()
            createVersion7Database(v7File)
            val database =
                Room
                    .databaseBuilder<AlongsideDatabase>(name = v7File.absolutePath)
                    .setDriver(BundledSQLiteDriver())
                    .setQueryCoroutineContext(Dispatchers.IO)
                    .addMigrations(MIGRATION_7_8, MIGRATION_8_9)
                    .build()
            try {
                val preMigrationEntry = database.diaryEntryDao().getById("entry-1")
                assertEquals(null, preMigrationEntry?.closedAt)

                val closedEntry = requireNotNull(preMigrationEntry).copy(closedAt = Instant.fromEpochMilliseconds(999))
                database.diaryEntryDao().upsert(closedEntry)

                val reloaded = database.diaryEntryDao().getById("entry-1")
                assertEquals(Instant.fromEpochMilliseconds(999), reloaded?.closedAt)
            } finally {
                database.close()
                v7File.delete()
            }
        }

    @Test
    fun `migration 8 to 9 widens photos primary key so two episodes can share a photo id`() =
        runTest {
            val v8File = File.createTempFile("migration-test-v8", ".db")
            v8File.delete()
            createVersion8Database(v8File)
            val database =
                Room
                    .databaseBuilder<AlongsideDatabase>(name = v8File.absolutePath)
                    .setDriver(BundledSQLiteDriver())
                    .setQueryCoroutineContext(Dispatchers.IO)
                    .addMigrations(MIGRATION_8_9)
                    .build()
            try {
                val preMigrationEpisode = database.episodeDao().getById("episode-1")
                assertEquals(listOf("photo-1"), preMigrationEpisode?.photos?.map { it.id })

                // The v8 bug: this same photo id, under a different episodeId, would have
                // silently replaced episode-1's row via INSERT OR REPLACE on the old single-
                // column PK. Post-migration the composite PK keeps both rows independent.
                val episodeTwo = requireNotNull(preMigrationEpisode).episode.copy(id = "episode-2")
                val photoForEpisodeTwo = preMigrationEpisode.photos.single().copy(episodeId = "episode-2")
                database.episodeDao().upsertEpisode(episodeTwo)
                database.episodeDao().upsertPhotos(listOf(photoForEpisodeTwo))

                assertPhotoIds(database, "episode-1", listOf("photo-1"))
                assertPhotoIds(database, "episode-2", listOf("photo-1"))
            } finally {
                database.close()
                v8File.delete()
            }
        }

    private suspend fun assertPhotoIds(
        database: AlongsideDatabase,
        episodeId: String,
        expected: List<String>,
    ) {
        assertEquals(
            expected,
            database
                .episodeDao()
                .getById(episodeId)
                ?.photos
                ?.map { it.id },
        )
    }

    private fun createVersion8Database(file: File) {
        val connection = BundledSQLiteDriver().open(file.absolutePath)
        try {
            connection.createVersion4SyncableTables()
            connection.createVersion8AuxiliaryTables()
            connection.insertVersion8Rows()
            connection.execSQL("PRAGMA user_version = 8")
        } finally {
            connection.close()
        }
    }

    private fun SQLiteConnection.createVersion8AuxiliaryTables() {
        execSQL(
            "CREATE TABLE IF NOT EXISTS `episodes` (`id` TEXT NOT NULL, `diaryEntryId` TEXT NOT NULL, " +
                "`startTime` INTEGER NOT NULL, `endTime` INTEGER NOT NULL, `latitude` REAL NOT NULL, " +
                "`longitude` REAL NOT NULL, `placeName` TEXT, `description` TEXT, " +
                "`descriptionAttempts` INTEGER NOT NULL, `syncStatus` TEXT NOT NULL DEFAULT 'PENDING', " +
                "`updatedAt` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`id`))",
        )
        execSQL(
            "CREATE INDEX IF NOT EXISTS `index_episodes_diaryEntryId` ON `episodes` (`diaryEntryId`)",
        )
        execSQL(
            "CREATE TABLE IF NOT EXISTS `photos` (`id` TEXT NOT NULL, `episodeId` TEXT NOT NULL, " +
                "`uri` TEXT NOT NULL, `takenAt` INTEGER NOT NULL, `latitude` REAL NOT NULL, " +
                "`longitude` REAL NOT NULL, `remoteUrl` TEXT, PRIMARY KEY(`id`), FOREIGN KEY(`episodeId`) " +
                "REFERENCES `episodes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        execSQL("CREATE INDEX IF NOT EXISTS `index_photos_episodeId` ON `photos` (`episodeId`)")
        // createVersion4SyncableTables already created diary_entries without closedAt (that
        // column doesn't exist until MIGRATION_7_8) - add it here to reach the v8 shape.
        execSQL("ALTER TABLE `diary_entries` ADD COLUMN `closedAt` INTEGER")
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

    private fun SQLiteConnection.insertVersion8Rows() {
        execSQL(
            "INSERT INTO episodes (id, diaryEntryId, startTime, endTime, latitude, longitude, " +
                "placeName, description, descriptionAttempts, syncStatus, updatedAt) VALUES " +
                "('episode-1', 'entry-1', 100, 200, 49.0, 24.0, 'Rynok Square', " +
                "'Wandering the old town', 0, 'PENDING', 200)",
        )
        execSQL(
            "INSERT INTO photos (id, episodeId, uri, takenAt, latitude, longitude, remoteUrl) VALUES " +
                "('photo-1', 'episode-1', 'content://photos/photo-1', 150, 49.0, 24.0, NULL)",
        )
    }

    private fun createVersion7Database(file: File) {
        val connection = BundledSQLiteDriver().open(file.absolutePath)
        try {
            connection.createVersion4SyncableTables()
            connection.createVersion7AuxiliaryTables()
            connection.insertVersion7Row()
            connection.execSQL("PRAGMA user_version = 7")
        } finally {
            connection.close()
        }
    }

    private fun SQLiteConnection.createVersion7AuxiliaryTables() {
        execSQL(
            "CREATE TABLE IF NOT EXISTS `episodes` (`id` TEXT NOT NULL, `diaryEntryId` TEXT NOT NULL, " +
                "`startTime` INTEGER NOT NULL, `endTime` INTEGER NOT NULL, `latitude` REAL NOT NULL, " +
                "`longitude` REAL NOT NULL, `placeName` TEXT, `description` TEXT, " +
                "`descriptionAttempts` INTEGER NOT NULL, `syncStatus` TEXT NOT NULL DEFAULT 'PENDING', " +
                "`updatedAt` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`id`))",
        )
        execSQL(
            "CREATE INDEX IF NOT EXISTS `index_episodes_diaryEntryId` ON `episodes` (`diaryEntryId`)",
        )
        execSQL(
            "CREATE TABLE IF NOT EXISTS `photos` (`id` TEXT NOT NULL, `episodeId` TEXT NOT NULL, " +
                "`uri` TEXT NOT NULL, `takenAt` INTEGER NOT NULL, `latitude` REAL NOT NULL, " +
                "`longitude` REAL NOT NULL, `remoteUrl` TEXT, PRIMARY KEY(`id`), FOREIGN KEY(`episodeId`) " +
                "REFERENCES `episodes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
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

    private fun SQLiteConnection.insertVersion7Row() {
        execSQL(
            "INSERT INTO diary_entries (id, tripId, userId, date, syncStatus, createdAt, updatedAt) " +
                "VALUES ('entry-1', 'trip-1', 'owner-1', '2026-07-16', 'SYNCED', 111, 111)",
        )
    }

    private fun createVersion6Database(file: File) {
        val connection = BundledSQLiteDriver().open(file.absolutePath)
        try {
            connection.createVersion4SyncableTables()
            connection.createVersion6AuxiliaryTables()
            connection.insertVersion6Rows()
            connection.execSQL("PRAGMA user_version = 6")
        } finally {
            connection.close()
        }
    }

    private fun SQLiteConnection.createVersion6AuxiliaryTables() {
        execSQL(
            "CREATE TABLE IF NOT EXISTS `episodes` (`id` TEXT NOT NULL, `diaryEntryId` TEXT NOT NULL, " +
                "`startTime` INTEGER NOT NULL, `endTime` INTEGER NOT NULL, `latitude` REAL NOT NULL, " +
                "`longitude` REAL NOT NULL, `placeName` TEXT, `description` TEXT, " +
                "`descriptionAttempts` INTEGER NOT NULL, `syncStatus` TEXT NOT NULL DEFAULT 'PENDING', " +
                "`updatedAt` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`id`))",
        )
        execSQL(
            "CREATE INDEX IF NOT EXISTS `index_episodes_diaryEntryId` ON `episodes` (`diaryEntryId`)",
        )
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

    private fun SQLiteConnection.insertVersion6Rows() {
        execSQL(
            "INSERT INTO episodes (id, diaryEntryId, startTime, endTime, latitude, longitude, " +
                "placeName, description, descriptionAttempts, syncStatus, updatedAt) VALUES " +
                "('episode-1', 'entry-1', 100, 200, 49.0, 24.0, 'Rynok Square', " +
                "'Wandering the old town', 0, 'PENDING', 200)",
        )
        execSQL(
            "INSERT INTO photos (id, episodeId, uri, takenAt, latitude, longitude) VALUES " +
                "('photo-1', 'episode-1', 'content://photos/photo-1', 150, 49.0, 24.0)",
        )
    }

    private fun createVersion5Database(file: File) {
        val connection = BundledSQLiteDriver().open(file.absolutePath)
        try {
            connection.createVersion4SyncableTables()
            connection.createVersion5AuxiliaryTables()
            connection.insertVersion5Row()
            connection.execSQL("PRAGMA user_version = 5")
        } finally {
            connection.close()
        }
    }

    private fun SQLiteConnection.createVersion5AuxiliaryTables() {
        execSQL(
            "CREATE TABLE IF NOT EXISTS `episodes` (`id` TEXT NOT NULL, `diaryEntryId` TEXT NOT NULL, " +
                "`startTime` INTEGER NOT NULL, `endTime` INTEGER NOT NULL, `latitude` REAL NOT NULL, " +
                "`longitude` REAL NOT NULL, `placeName` TEXT, `description` TEXT, " +
                "`descriptionAttempts` INTEGER NOT NULL, PRIMARY KEY(`id`))",
        )
        execSQL(
            "CREATE INDEX IF NOT EXISTS `index_episodes_diaryEntryId` ON `episodes` (`diaryEntryId`)",
        )
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

    private fun SQLiteConnection.insertVersion5Row() {
        execSQL(
            "INSERT INTO episodes (id, diaryEntryId, startTime, endTime, latitude, longitude, " +
                "placeName, description, descriptionAttempts) VALUES ('episode-1', 'entry-1', 100, 200, " +
                "49.0, 24.0, 'Rynok Square', 'Wandering the old town', 0)",
        )
    }

    private fun createVersion4Database(file: File) {
        val connection = BundledSQLiteDriver().open(file.absolutePath)
        try {
            connection.createVersion4SyncableTables()
            connection.createVersion4AuxiliaryTables()
            connection.insertVersion4Row()
            connection.execSQL("PRAGMA user_version = 4")
        } finally {
            connection.close()
        }
    }

    private fun SQLiteConnection.createVersion4SyncableTables() {
        execSQL(
            "CREATE TABLE IF NOT EXISTS `trips` (`id` TEXT NOT NULL, `ownerId` TEXT NOT NULL, " +
                "`memberId` TEXT, `inviteCode` TEXT NOT NULL, `startDate` TEXT NOT NULL, " +
                "`endDate` TEXT NOT NULL, `syncStatus` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, " +
                "`updatedAt` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`id`))",
        )
        execSQL(
            "CREATE TABLE IF NOT EXISTS `diary_entries` (`id` TEXT NOT NULL, `tripId` TEXT NOT NULL, " +
                "`userId` TEXT NOT NULL, `date` TEXT NOT NULL, `syncStatus` TEXT NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`id`))",
        )
        execSQL("CREATE INDEX IF NOT EXISTS `index_diary_entries_tripId` ON `diary_entries` (`tripId`)")
        execSQL(
            "CREATE TABLE IF NOT EXISTS `place_candidates` (`id` TEXT NOT NULL, `tripId` TEXT NOT NULL, " +
                "`name` TEXT NOT NULL, `latitude` REAL NOT NULL, `longitude` REAL NOT NULL, `note` TEXT, " +
                "`addedByUserId` TEXT NOT NULL, `ownerSwipe` TEXT, `memberSwipe` TEXT, " +
                "`syncStatus` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, " +
                "`updatedAt` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`id`))",
        )
        execSQL(
            "CREATE INDEX IF NOT EXISTS `index_place_candidates_tripId` ON `place_candidates` (`tripId`)",
        )
        execSQL(
            "CREATE TABLE IF NOT EXISTS `sync_operations` " +
                "(`seq` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `opId` TEXT NOT NULL, " +
                "`collectionPath` TEXT NOT NULL, `documentId` TEXT NOT NULL, `type` TEXT NOT NULL, " +
                "`fieldsJson` TEXT NOT NULL, `attempts` INTEGER NOT NULL, `status` TEXT NOT NULL, " +
                "`enqueuedAt` INTEGER NOT NULL)",
        )
        execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_sync_operations_opId` ON `sync_operations` (`opId`)",
        )
    }

    private fun SQLiteConnection.createVersion4AuxiliaryTables() {
        execSQL(
            "CREATE TABLE IF NOT EXISTS `episodes` (`id` TEXT NOT NULL, `diaryEntryId` TEXT NOT NULL, " +
                "`startTime` INTEGER NOT NULL, `endTime` INTEGER NOT NULL, `latitude` REAL NOT NULL, " +
                "`longitude` REAL NOT NULL, `placeName` TEXT, `description` TEXT, PRIMARY KEY(`id`))",
        )
        execSQL(
            "CREATE INDEX IF NOT EXISTS `index_episodes_diaryEntryId` ON `episodes` (`diaryEntryId`)",
        )
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

    private fun SQLiteConnection.insertVersion4Row() {
        execSQL(
            "INSERT INTO episodes (id, diaryEntryId, startTime, endTime, latitude, longitude, " +
                "placeName, description) VALUES ('episode-1', 'entry-1', 100, 200, 49.0, 24.0, " +
                "'Rynok Square', 'Wandering the old town')",
        )
    }
}
