package com.alongside.core.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

public fun getDatabaseBuilder(context: Context): RoomDatabase.Builder<AlongsideDatabase> {
    val appContext = context.applicationContext
    val dbFile = appContext.getDatabasePath(DATABASE_FILE_NAME)
    return Room.databaseBuilder<AlongsideDatabase>(
        context = appContext,
        name = dbFile.absolutePath,
    )
}
