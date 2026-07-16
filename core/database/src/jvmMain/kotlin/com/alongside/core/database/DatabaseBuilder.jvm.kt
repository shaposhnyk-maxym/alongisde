package com.alongside.core.database

import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File

public fun getDatabaseBuilder(): RoomDatabase.Builder<AlongsideDatabase> {
    val dbFile = File(System.getProperty("java.io.tmpdir"), DATABASE_FILE_NAME)
    return Room.databaseBuilder<AlongsideDatabase>(name = dbFile.absolutePath)
}
