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
