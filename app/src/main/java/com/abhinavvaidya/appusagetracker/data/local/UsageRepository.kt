package com.abhinavvaidya.appusagetracker.data.local

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.Log
import com.abhinavvaidya.appusagetracker.domain.model.AppUsageInfo
import com.abhinavvaidya.appusagetracker.domain.model.DailyUsageSummary
import com.abhinavvaidya.appusagetracker.system.usage.UsageStatsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Repository for managing app usage data.
 *
 * This class acts as a single source of truth for usage data by:
 * 1. Fetching fresh data from UsageStatsHelper
 * 2. Caching data in Room database for offline access
 * 3. Providing Flow-based APIs for reactive UI updates
 * 4. Managing widget cache for battery-efficient widget updates
 *
 * BATTERY OPTIMIZATION:
 * - Widget reads ONLY from cached data, never directly from UsageStats
 * - Cache is updated at controlled intervals via WorkManager
 * - Smart cache invalidation based on date changes
 */
class UsageRepository(context: Context) {

    companion object {
        private const val TAG = "UsageRepository"
    }

    private val database = AppUsageDatabase.getDatabase(context)
    private val dao = database.appUsageDao()
    private val widgetCacheDao = database.widgetCacheDao()
    private val usageStatsHelper = UsageStatsHelper(context)
    private val packageManager: PackageManager = context.packageManager

    init {
        Log.d(TAG, "UsageRepository initialized, packageManager=$packageManager")
    }

    /**
     * Loads the app icon for the given package name.
     * Returns null if the package is not found (common for system/virtual packages in emulators).
     */
    private fun getAppIcon(packageName: String): Drawable? {
        return try {
            packageManager.getApplicationIcon(packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            // Package not found - this is expected for some system packages
            // or uninstalled apps that still show in usage stats
            Log.v(TAG, "Package $packageName not found for icon loading")
            null
        } catch (e: Exception) {
            Log.w(TAG, "Error loading icon for $packageName: ${e.message}")
            null
        }
    }

    /**
     * Checks if the app has Usage Access permission.
     */
    fun hasUsagePermission(): Boolean = usageStatsHelper.hasUsagePermission()

    /**
     * Refreshes today's usage data from the system and caches it in the database.
     * This should be called before reading data to ensure freshness.
     */
    suspend fun refreshTodayUsage() = withContext(Dispatchers.IO) {
        Log.d(TAG, "Refreshing today's usage data")

        // Check permission first
        if (!hasUsagePermission()) {
            Log.w(TAG, "Cannot refresh: No usage permission")
            return@withContext
        }

        val today = usageStatsHelper.getDateString()
        val usageList = usageStatsHelper.getTodayUsage()

        Log.d(TAG, "Fetched ${usageList.size} apps for today ($today)")

        // Clear old data for today and insert fresh data
        dao.deleteUsageForDate(today)

        if (usageList.isNotEmpty()) {
            val entities = usageList.map { usage ->
                AppUsageEntity(
                    packageName = usage.packageName,
                    appName = usage.appName,
                    usageTimeMillis = usage.usageTimeMillis,
                    date = today
                )
            }
            dao.insertAll(entities)
            Log.d(TAG, "Cached ${entities.size} usage entries for $today")
        }
    }

    /**
     * Refreshes weekly usage data (last 7 days) from the system.
     */
    suspend fun refreshWeeklyUsage() = withContext(Dispatchers.IO) {
        Log.d(TAG, "Refreshing weekly usage data")

        if (!hasUsagePermission()) {
            Log.w(TAG, "Cannot refresh: No usage permission")
            return@withContext
        }

        for (daysAgo in 0..6) {
            val date = usageStatsHelper.getDateString(daysAgo)
            val usageList = usageStatsHelper.getUsageForDate(daysAgo)

            Log.d(TAG, "Day $daysAgo ($date): ${usageList.size} apps")

            dao.deleteUsageForDate(date)

            if (usageList.isNotEmpty()) {
                val entities = usageList.map { usage ->
                    AppUsageEntity(
                        packageName = usage.packageName,
                        appName = usage.appName,
                        usageTimeMillis = usage.usageTimeMillis,
                        date = date
                    )
                }
                dao.insertAll(entities)
            }
        }
    }

    /**
     * Returns a Flow of today's usage data from the database.
     * Subscribe to this for reactive UI updates.
     */
    fun getTodayUsageFlow(): Flow<List<AppUsageInfo>> {
        val today = usageStatsHelper.getDateString()
        return dao.getUsageForDate(today).map { entities ->
            entities.map { entity ->
                AppUsageInfo(
                    packageName = entity.packageName,
                    appName = entity.appName,
                    usageTimeMillis = entity.usageTimeMillis,
                    appIcon = getAppIcon(entity.packageName)
                )
            }
        }
    }

    /**
     * Returns a Flow of weekly usage summaries.
     */
    fun getWeeklyUsageFlow(): Flow<List<DailyUsageSummary>> {
        val dates = usageStatsHelper.getLast7DaysDates()
        return dao.getUsageForDates(dates).map { entities ->
            entities.groupBy { it.date }
                .map { (date, dayEntities) ->
                    DailyUsageSummary(
                        date = date,
                        totalTimeMillis = dayEntities.sumOf { it.usageTimeMillis },
                        apps = dayEntities.map { entity ->
                            AppUsageInfo(
                                packageName = entity.packageName,
                                appName = entity.appName,
                                usageTimeMillis = entity.usageTimeMillis,
                                appIcon = getAppIcon(entity.packageName)
                            )
                        }
                    )
                }
                .sortedByDescending { it.date }
        }
    }

    /**
     * Returns today's usage data directly from the system (not from cache).
     * Use this for the most up-to-date data.
     */
    fun getTodayUsageDirect(): List<AppUsageInfo> {
        Log.d(TAG, "Getting today's usage directly from system")
        return usageStatsHelper.getTodayUsage()
    }

    // ==================== WIDGET CACHE METHODS ====================
    // These methods support battery-efficient widget updates

    /**
     * Gets cached widget data for today.
     * Widget should ALWAYS use this instead of querying UsageStats directly.
     *
     * @return WidgetCacheEntity or null if no cache exists
     */
    suspend fun getWidgetCache(): WidgetCacheEntity? = withContext(Dispatchers.IO) {
        val today = usageStatsHelper.getDateString()
        widgetCacheDao.getCacheForDate(today)
    }

    /**
     * Checks if widget cache is fresh enough to use.
     *
     * @param maxAgeMinutes Maximum age of cache in minutes
     * @return true if cache exists and is fresh enough
     */
    suspend fun isWidgetCacheFresh(maxAgeMinutes: Int = 30): Boolean = withContext(Dispatchers.IO) {
        val today = usageStatsHelper.getDateString()
        val cache = widgetCacheDao.getCacheForDate(today)

        if (cache == null) {
            Log.d(TAG, "Widget cache miss: no cache for today")
            return@withContext false
        }

        val ageMinutes = (System.currentTimeMillis() - cache.lastUpdatedTimestamp) / (1000 * 60)
        val isFresh = ageMinutes < maxAgeMinutes

        Log.d(TAG, "Widget cache age: ${ageMinutes}m, fresh: $isFresh (max: ${maxAgeMinutes}m)")
        isFresh
    }

    /**
     * Refreshes widget cache with current usage data.
     * Called by WorkManager at controlled intervals.
     *
     * BATTERY OPTIMIZATION:
     * - Only queries UsageStats when cache is stale
     * - Computes summary data once and caches it
     * - Widget reads from cache, avoiding repeated system queries
     */
    suspend fun refreshWidgetCache() = withContext(Dispatchers.IO) {
        Log.d(TAG, "Refreshing widget cache")

        if (!hasUsagePermission()) {
            Log.w(TAG, "Cannot refresh widget cache: No usage permission")
            return@withContext
        }

        // Get fresh usage data
        val usageList = usageStatsHelper.getTodayUsage()

        // Calculate totals
        val totalMillis = usageList.sumOf { it.usageTimeMillis }
        val topApp = usageList.maxByOrNull { it.usageTimeMillis }
        val today = usageStatsHelper.getDateString()

        // Create cache entry
        val cacheEntry = WidgetCacheEntity(
            dateString = today,
            totalScreenTimeMillis = totalMillis,
            topAppPackageName = topApp?.packageName,
            topAppName = topApp?.appName,
            topAppUsageMillis = topApp?.usageTimeMillis ?: 0L,
            lastUpdatedTimestamp = System.currentTimeMillis(),
            usageLevel = WidgetCacheEntity.calculateUsageLevel(totalMillis)
        )

        widgetCacheDao.upsertCache(cacheEntry)

        // Cleanup old cache entries
        widgetCacheDao.cleanupOldCache()

        Log.d(TAG, "Widget cache updated: ${totalMillis / (1000 * 60)}m total, top: ${topApp?.appName}")
    }

    /**
     * Gets the current date string for cache key.
     */
    fun getTodayDateString(): String = usageStatsHelper.getDateString()
}

