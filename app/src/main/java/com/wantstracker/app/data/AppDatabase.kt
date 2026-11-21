package com.wantstracker.app.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [CategoryEntity::class, WantEntity::class, StatusLogEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun wantDao(): WantDao
    abstract fun logDao(): LogDao
}
