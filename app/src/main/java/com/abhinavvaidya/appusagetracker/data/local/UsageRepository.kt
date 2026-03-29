package com.abhinavvaidya.appusagetracker.data.local

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import com.abhinavvaidya.appusagetracker.data.preferences.WidgetPreferencesRepository
import com.abhinavvaidya.appusagetracker.domain.model.AppUsageInfo
import com.abhinavvaidya.appusagetracker.domain.model.DailyUsageSummary
import com.abhinavvaidya.appusagetracker.system.usage.UsageStatsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

data class DailyUsageSnapshot(
    val usageList: List<AppUsageInfo>,
    val launchCount: Int,
    val unlockCount: Int,
    val notificationCount: Int,
    val timeFragmentPercent: Int
)

class UsageRepository(context: Context) {

    companion object {
        private const val TAG = "UsageRepository"
    }

    private val database = AppUsageDatabase.getDatabase(context)
    private val dao = database.appUsageDao()
    private val widgetCacheDao = database.widgetCacheDao()
    private val usageStatsHelper = UsageStatsHelper(context)
    private val packageManager: PackageManager = context.packageManager
    private val preferencesRepository = WidgetPreferencesRepository(context)

    fun hasUsagePermission(): Boolean = usageStatsHelper.hasUsagePermission()

    suspend fun refreshTodayUsage() = withContext(Dispatchers.IO) {
        if (!hasUsagePermission()) return@withContext

        val today = usageStatsHelper.getDateString()
        val usageList = applyDisplayFilters(usageStatsHelper.getTodayUsage())
        persistUsageForDate(today, usageList)
    }

    suspend fun refreshWeeklyUsage() = withContext(Dispatchers.IO) {
        if (!hasUsagePermission()) return@withContext

        for (daysAgo in 0..6) {
            val date = usageStatsHelper.getDateString(daysAgo)
            val usageList = applyDisplayFilters(usageStatsHelper.getUsageForDate(daysAgo))
            persistUsageForDate(date, usageList)
        }
    }

    suspend fun getUsageSnapshotForDate(daysAgo: Int): DailyUsageSnapshot = withContext(Dispatchers.IO) {
        val safeDaysAgo = daysAgo.coerceAtLeast(0)
        val date = usageStatsHelper.getDateString(safeDaysAgo)
        val liveUsageList = applyDisplayFilters(usageStatsHelper.getUsageForDate(safeDaysAgo))

        val usageList = when {
            liveUsageList.isNotEmpty() -> liveUsageList
            safeDaysAgo == 0 -> liveUsageList
            else -> getCachedUsageForDate(date)
        }

        if (liveUsageList.isNotEmpty() || safeDaysAgo == 0) {
            persistUsageForDate(date, liveUsageList)
        }

        if (safeDaysAgo == 0) {
            refreshWidgetCacheFromUsage(liveUsageList, date)
        }

        val behaviorMetrics = usageStatsHelper.getBehaviorMetricsForDate(safeDaysAgo)
        val metricsFromEventsAvailable = liveUsageList.isNotEmpty() || safeDaysAgo == 0

        DailyUsageSnapshot(
            usageList = usageList,
            launchCount = usageList.sumOf { it.launchCount },
            unlockCount = if (metricsFromEventsAvailable) behaviorMetrics.unlockCount else 0,
            notificationCount = if (metricsFromEventsAvailable) behaviorMetrics.notificationCount else 0,
            timeFragmentPercent = if (metricsFromEventsAvailable) behaviorMetrics.timeFragmentPercent else 0
        )
    }

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

    suspend fun getTodayUsageDirect(): List<AppUsageInfo> = withContext(Dispatchers.IO) {
        applyDisplayFilters(usageStatsHelper.getTodayUsage())
    }

    suspend fun getUsageForDateDirect(daysAgo: Int): List<AppUsageInfo> = withContext(Dispatchers.IO) {
        applyDisplayFilters(usageStatsHelper.getUsageForDate(daysAgo.coerceAtLeast(0)))
    }

    suspend fun getWidgetCache(): WidgetCacheEntity? = withContext(Dispatchers.IO) {
        val today = usageStatsHelper.getDateString()
        widgetCacheDao.getCacheForDate(today)
    }

    suspend fun isWidgetCacheFresh(maxAgeMinutes: Int = 30): Boolean = withContext(Dispatchers.IO) {
        val today = usageStatsHelper.getDateString()
        val cache = widgetCacheDao.getCacheForDate(today) ?: return@withContext false

        val ageMinutes = (System.currentTimeMillis() - cache.lastUpdatedTimestamp) / (1000 * 60)
        ageMinutes < maxAgeMinutes
    }

    suspend fun refreshWidgetCache() = withContext(Dispatchers.IO) {
        if (!hasUsagePermission()) return@withContext

        val today = usageStatsHelper.getDateString()
        val usageList = applyDisplayFilters(usageStatsHelper.getTodayUsage())
        refreshWidgetCacheFromUsage(usageList, today)
    }

    fun getTodayDateString(): String = usageStatsHelper.getDateString()

    fun getDateString(daysAgo: Int): String = usageStatsHelper.getDateString(daysAgo)

    private suspend fun applyDisplayFilters(usageList: List<AppUsageInfo>): List<AppUsageInfo> {
        if (usageList.isEmpty()) return usageList

        val excludeSystemApps = preferencesRepository.getExcludeSystemApps()
        val excludedPackages = preferencesRepository.getExcludedPackages()

        val filtered = usageList
            .filterNot { excludedPackages.contains(it.packageName) }
            .filterNot { excludeSystemApps && isSystemApp(it.packageName) }
            .sortedByDescending { it.usageTimeMillis }

        val totalMillis = filtered.sumOf { it.usageTimeMillis }
        if (totalMillis <= 0L) {
            return filtered
        }

        return filtered.mapIndexed { index, app ->
            app.copy(
                usagePercentage = (app.usageTimeMillis.toFloat() / totalMillis.toFloat()) * 100f,
                rank = index + 1
            )
        }
    }

    private suspend fun getCachedUsageForDate(date: String): List<AppUsageInfo> {
        val cached = dao.getUsageForDateOnce(date)
            .map { entity ->
                AppUsageInfo(
                    packageName = entity.packageName,
                    appName = entity.appName,
                    usageTimeMillis = entity.usageTimeMillis,
                    appIcon = getAppIcon(entity.packageName)
                )
            }
            .sortedByDescending { it.usageTimeMillis }

        val totalMillis = cached.sumOf { it.usageTimeMillis }
        if (totalMillis <= 0L) return cached

        return cached.mapIndexed { index, app ->
            app.copy(
                usagePercentage = (app.usageTimeMillis.toFloat() / totalMillis.toFloat()) * 100f,
                rank = index + 1
            )
        }
    }

    private suspend fun persistUsageForDate(date: String, usageList: List<AppUsageInfo>) {
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

    private suspend fun refreshWidgetCacheFromUsage(usageList: List<AppUsageInfo>, date: String) {
        val totalMillis = usageList.sumOf { it.usageTimeMillis }
        val topApp = usageList.maxByOrNull { it.usageTimeMillis }

        val cacheEntry = WidgetCacheEntity(
            dateString = date,
            totalScreenTimeMillis = totalMillis,
            topAppPackageName = topApp?.packageName,
            topAppName = topApp?.appName,
            topAppUsageMillis = topApp?.usageTimeMillis ?: 0L,
            lastUpdatedTimestamp = System.currentTimeMillis(),
            usageLevel = WidgetCacheEntity.calculateUsageLevel(totalMillis)
        )

        widgetCacheDao.upsertCache(cacheEntry)
        widgetCacheDao.cleanupOldCache()

        Log.d(TAG, "Widget cache updated: ${totalMillis / (1000 * 60)}m")
    }

    private fun getAppIcon(packageName: String): Drawable? {
        return try {
            packageManager.getApplicationIcon(packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            getLaunchAppIcon(packageName)
        } catch (_: Exception) {
            null
        }
    }

    private fun getLaunchAppIcon(packageName: String): Drawable? {
        return try {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName) ?: return null
            val resolveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.resolveActivity(launchIntent, PackageManager.ResolveInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.resolveActivity(launchIntent, 0)
            }
            resolveInfo?.loadIcon(packageManager)
        } catch (_: Exception) {
            null
        }
    }

    private fun isSystemApp(packageName: String): Boolean {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val isUpdatedSystem = (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
            isSystem || isUpdatedSystem
        } catch (_: PackageManager.NameNotFoundException) {
            false
        } catch (_: Exception) {
            false
        }
    }
}
