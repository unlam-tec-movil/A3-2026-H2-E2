package com.trazo.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [SavedRunEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun savedRunDao(): SavedRunDao
}
