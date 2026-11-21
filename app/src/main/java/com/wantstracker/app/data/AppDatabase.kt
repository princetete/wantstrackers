package com.wantstracker.app.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [CategoryEntity::class, WantEntity::class, StatusLogEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE wants ADD COLUMN emoji TEXT NOT NULL DEFAULT '🛍️'")
            }
        }
    }
    abstract fun categoryDao(): CategoryDao
    abstract fun wantDao(): WantDao
    abstract fun logDao(): LogDao
}
