package com.alongside.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * v3 -> v4 (M9): updatedAt on syncable tables (backfilled from createdAt) +
 * the persistent sync_operations queue.
 */
internal val MIGRATION_3_4: Migration =
    object : Migration(3, 4) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE `trips` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0")
            connection.execSQL("UPDATE `trips` SET `updatedAt` = `createdAt`")
            connection.execSQL("ALTER TABLE `diary_entries` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0")
            connection.execSQL("UPDATE `diary_entries` SET `updatedAt` = `createdAt`")
            connection.execSQL("ALTER TABLE `place_candidates` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0")
            connection.execSQL("UPDATE `place_candidates` SET `updatedAt` = `createdAt`")
            connection.execSQL(
                "CREATE TABLE IF NOT EXISTS `sync_operations` " +
                    "(`seq` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `opId` TEXT NOT NULL, " +
                    "`collectionPath` TEXT NOT NULL, `documentId` TEXT NOT NULL, `type` TEXT NOT NULL, " +
                    "`fieldsJson` TEXT NOT NULL, `attempts` INTEGER NOT NULL, `status` TEXT NOT NULL, " +
                    "`enqueuedAt` INTEGER NOT NULL)",
            )
            connection.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_sync_operations_opId` ON `sync_operations` (`opId`)",
            )
        }
    }

/** v4 -> v5 (M10): descriptionAttempts on episodes, backing the per-episode regeneration limit. */
internal val MIGRATION_4_5: Migration =
    object : Migration(4, 5) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE `episodes` ADD COLUMN `descriptionAttempts` INTEGER NOT NULL DEFAULT 0")
        }
    }

/**
 * v5 -> v6 (M11): syncStatus + updatedAt on episodes, so Episode can join Trip/DiaryEntry in the
 * sync-queue pipeline. Episode had no createdAt to backfill updatedAt from (unlike M9's trio) -
 * endTime is the closest existing timestamp. Pre-M11 local episodes haven't been pushed to
 * Firestore yet, so PENDING is the honest starting syncStatus, not a placeholder to overwrite.
 */
internal val MIGRATION_5_6: Migration =
    object : Migration(5, 6) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE `episodes` ADD COLUMN `syncStatus` TEXT NOT NULL DEFAULT 'PENDING'")
            connection.execSQL("ALTER TABLE `episodes` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0")
            connection.execSQL("UPDATE `episodes` SET `updatedAt` = `endTime`")
        }
    }

/**
 * v6 -> v7 (M12.5): photos.remoteUrl, the Firebase Storage download URL for a synced photo.
 * Nullable with no DEFAULT clause (first migration of this shape in this file) - SQLite leaves
 * every existing row's remoteUrl as NULL, which is the correct "not yet uploaded" state for
 * photos captured before this migration ships.
 */
internal val MIGRATION_6_7: Migration =
    object : Migration(6, 7) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE `photos` ADD COLUMN `remoteUrl` TEXT")
        }
    }

/**
 * v7 -> v8 (M12.6): diary_entries.closedAt, the explicit "I'm done for today" marker driving
 * the day-unlock rule. Nullable with no DEFAULT clause, same shape as v6->v7's remoteUrl -
 * every existing row's closedAt stays NULL, the correct "not yet closed" state.
 */
internal val MIGRATION_7_8: Migration =
    object : Migration(7, 8) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE `diary_entries` ADD COLUMN `closedAt` INTEGER")
        }
    }

/**
 * v8 -> v9 (M12.6 bugfix): photos' primary key widens from (id) alone to (id, episodeId) - see
 * PhotoEntity's KDoc for why a single-column PK let two different episodes' photo rows collide
 * and get silently stolen from each other on every poll tick. SQLite can't widen a primary key
 * in place, so the table is recreated and repopulated.
 */
internal val MIGRATION_8_9: Migration =
    object : Migration(8, 9) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE `photos` RENAME TO `photos_old`")
            connection.execSQL(
                "CREATE TABLE `photos` (`id` TEXT NOT NULL, `episodeId` TEXT NOT NULL, " +
                    "`uri` TEXT NOT NULL, `takenAt` INTEGER NOT NULL, `latitude` REAL NOT NULL, " +
                    "`longitude` REAL NOT NULL, `remoteUrl` TEXT, PRIMARY KEY(`id`, `episodeId`), " +
                    "FOREIGN KEY(`episodeId`) REFERENCES `episodes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            )
            connection.execSQL(
                "INSERT INTO `photos` (id, episodeId, uri, takenAt, latitude, longitude, remoteUrl) " +
                    "SELECT id, episodeId, uri, takenAt, latitude, longitude, remoteUrl FROM `photos_old`",
            )
            connection.execSQL("DROP TABLE `photos_old`")
            connection.execSQL("CREATE INDEX IF NOT EXISTS `index_photos_episodeId` ON `photos` (`episodeId`)")
        }
    }
