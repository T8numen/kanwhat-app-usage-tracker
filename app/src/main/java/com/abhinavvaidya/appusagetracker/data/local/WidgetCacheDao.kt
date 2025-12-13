package com.abhinavvaidya.appusagetracker.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * DAO for widget cache operations.
 *
 * DESIGN DECISIONS:
 * - Uses Upsert to simplify insert/update logic
 * - Provides both Flow and suspend functions for different use cases
 * - Flow for reactive UI, suspend for one-shot widget updates
 */
@Dao
interface WidgetCacheDao {

    /**
     * Inserts or updates widget cache for a specific date.
     * Used by WorkManager when refreshing widget data.
     */
    @Upsert
    suspend fun upsertCache(cache: WidgetCacheEntity)

    /**
     * Gets cached data for a specific date.
     * Returns null if no cache exists for that date.
     */
    @Query("SELECT * FROM widget_cache WHERE dateString = :date LIMIT 1")
    suspend fun getCacheForDate(date: String): WidgetCacheEntity?

    /**
     * Gets cached data as Flow for reactive updates.
     * Used by widget to observe changes.
     */
    @Query("SELECT * FROM widget_cache WHERE dateString = :date LIMIT 1")
    fun getCacheForDateFlow(date: String): Flow<WidgetCacheEntity?>

    /**
     * Gets the most recent cache entry.
     * Useful for widget to show "last updated" timestamp.
     */
    @Query("SELECT * FROM widget_cache ORDER BY lastUpdatedTimestamp DESC LIMIT 1")
    suspend fun getLatestCache(): WidgetCacheEntity?

    /**
     * Deletes old cache entries to prevent database bloat.
     * Keeps only last 7 days of data.
     */
    @Query("DELETE FROM widget_cache WHERE dateString NOT IN (SELECT dateString FROM widget_cache ORDER BY dateString DESC LIMIT 7)")
    suspend fun cleanupOldCache()
}

