package com.abhinavvaidya.appusagetracker.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity for caching widget data to avoid repeated UsageStats queries.
 *
 * BATTERY OPTIMIZATION:
 * - Widget reads ONLY from this cache, never from UsageStatsManager directly
 * - Cache is updated by WorkManager at controlled intervals (30-60 min)
 * - This prevents battery drain from frequent usage queries
 */
@Entity(tableName = "widget_cache")
data class WidgetCacheEntity(
    @PrimaryKey
    val dateString: String,                    // Format: yyyy-MM-dd
    val totalScreenTimeMillis: Long,           // Total screen time for the day
    val topAppPackageName: String?,            // Package name of most used app
    val topAppName: String?,                   // Display name of most used app
    val topAppUsageMillis: Long,               // Usage time of top app
    val lastUpdatedTimestamp: Long,            // When cache was last refreshed
    val usageLevel: Int = 0                    // 0=Low, 1=Medium, 2=High (for gamification)
) {
    companion object {
        // Usage thresholds for gamification (in milliseconds)
        const val LOW_USAGE_THRESHOLD = 2 * 60 * 60 * 1000L      // 2 hours
        const val MEDIUM_USAGE_THRESHOLD = 4 * 60 * 60 * 1000L   // 4 hours

        /**
         * Calculates usage level for gamified visuals
         * 0 = Low (green) - under 2 hours
         * 1 = Medium (yellow) - 2-4 hours
         * 2 = High (red) - over 4 hours
         */
        fun calculateUsageLevel(totalMillis: Long): Int {
            return when {
                totalMillis < LOW_USAGE_THRESHOLD -> 0
                totalMillis < MEDIUM_USAGE_THRESHOLD -> 1
                else -> 2
            }
        }
    }
}

