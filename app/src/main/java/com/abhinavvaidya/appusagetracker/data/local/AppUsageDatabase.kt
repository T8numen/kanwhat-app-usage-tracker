package com.abhinavvaidya.appusagetracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room database for app usage tracking.
 *
 * VERSION HISTORY:
 * - v1: Initial release with AppUsageEntity
 * - v2: Added WidgetCacheEntity for battery-efficient widget updates
 */
@Database(
    entities = [AppUsageEntity::class, WidgetCacheEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppUsageDatabase : RoomDatabase() {
    abstract fun appUsageDao(): AppUsageDao
    abstract fun widgetCacheDao(): WidgetCacheDao

    companion object {
        @Volatile
        private var INSTANCE: AppUsageDatabase? = null

        /**
         * Migration from v1 to v2: Add widget_cache table
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS widget_cache (
                        dateString TEXT NOT NULL PRIMARY KEY,
                        totalScreenTimeMillis INTEGER NOT NULL,
                        topAppPackageName TEXT,
                        topAppName TEXT,
                        topAppUsageMillis INTEGER NOT NULL,
                        lastUpdatedTimestamp INTEGER NOT NULL,
                        usageLevel INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
            }
        }

        fun getDatabase(context: Context): AppUsageDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppUsageDatabase::class.java,
                    "app_usage_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

