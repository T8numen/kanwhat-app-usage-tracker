package com.abhinavvaidya.appusagetracker.system.usage

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Process
import android.util.Log
import com.abhinavvaidya.appusagetracker.domain.model.AppUsageInfo
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class DailyBehaviorMetrics(
    val launchCount: Int,
    val unlockCount: Int,
    val notificationCount: Int,
    val timeFragmentPercent: Int
)

class UsageStatsHelper(private val context: Context) {

    private val usageStatsManager: UsageStatsManager? by lazy {
        try {
            context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get UsageStatsManager", e)
            null
        }
    }

    private val packageManager: PackageManager by lazy {
        context.packageManager
    }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private data class LauncherAppEntry(
        val label: String,
        val icon: Drawable?
    )

    private data class UsageEventsData(
        val durations: Map<String, Long>,
        val launches: Map<String, Int>,
        val totalLaunches: Int,
        val unlockCount: Int,
        val notificationCount: Int,
        val totalSessions: Int,
        val shortSessions: Int
    )

    private val launcherAppCache: Map<String, LauncherAppEntry> by lazy {
        loadLauncherAppCache()
    }

    companion object {
        private const val TAG = "UsageStatsHelper"
        private const val MIN_USAGE_MS = 30_000L
        private const val SHORT_SESSION_MS = 60_000L
        private const val EVENT_NOTIFICATION_INTERRUPTION = 12
    }

    fun hasUsagePermission(): Boolean {
        return try {
            val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
                ?: return false

            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOpsManager.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOpsManager.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            }

            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            Log.e(TAG, "Error checking usage permission", e)
            false
        }
    }

    fun getTodayUsage(): List<AppUsageInfo> {
        if (!hasUsagePermission()) {
            Log.w(TAG, "getTodayUsage: No usage permission")
            return emptyList()
        }

        val (startTime, endTime) = getTimeRange(0)
        return getUsageForTimeRange(startTime, endTime, isToday = true)
    }

    fun getUsageForDate(daysAgo: Int): List<AppUsageInfo> {
        if (!hasUsagePermission()) {
            Log.w(TAG, "getUsageForDate: No usage permission")
            return emptyList()
        }

        val safeDaysAgo = daysAgo.coerceAtLeast(0)
        val (startTime, endTime) = getTimeRange(safeDaysAgo)
        return getUsageForTimeRange(startTime, endTime, isToday = safeDaysAgo == 0)
    }

    fun getBehaviorMetricsForDate(daysAgo: Int): DailyBehaviorMetrics {
        if (!hasUsagePermission()) {
            return DailyBehaviorMetrics(
                launchCount = 0,
                unlockCount = 0,
                notificationCount = 0,
                timeFragmentPercent = 0
            )
        }

        val safeDaysAgo = daysAgo.coerceAtLeast(0)
        val (startTime, endTime) = getTimeRange(safeDaysAgo)
        val eventsData = getUsageFromEvents(startTime, endTime)

        val fragmentPercent = if (eventsData.totalSessions > 0) {
            ((eventsData.shortSessions * 100f) / eventsData.totalSessions.toFloat()).toInt().coerceIn(0, 100)
        } else {
            0
        }

        return DailyBehaviorMetrics(
            launchCount = eventsData.totalLaunches,
            unlockCount = eventsData.unlockCount,
            notificationCount = eventsData.notificationCount,
            timeFragmentPercent = fragmentPercent
        )
    }

    private fun getTimeRange(daysAgo: Int): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -daysAgo)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis

        val endTime = if (daysAgo == 0) {
            System.currentTimeMillis()
        } else {
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            calendar.timeInMillis
        }

        return startTime to endTime
    }

    private fun getUsageForTimeRange(startTime: Long, endTime: Long, isToday: Boolean): List<AppUsageInfo> {
        val statsManager = usageStatsManager ?: return emptyList()

        val aggregatedStats = mutableMapOf<String, Long>()
        val launchCounts = mutableMapOf<String, Int>()

        try {
            val dailyStats = statsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
            )

            dailyStats?.forEach { stats ->
                val packageName = normalizePackageName(stats.packageName)
                if (packageName.isBlank()) return@forEach

                val totalTime = stats.totalTimeInForeground
                if (totalTime > 0) {
                    aggregatedStats[packageName] = (aggregatedStats[packageName] ?: 0L) + totalTime
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying INTERVAL_DAILY usage stats", e)
        }

        try {
            val eventsData = getUsageFromEvents(startTime, endTime)

            if (isToday || aggregatedStats.isEmpty()) {
                for ((packageName, time) in eventsData.durations) {
                    val current = aggregatedStats[packageName] ?: 0L
                    aggregatedStats[packageName] = maxOf(current, time)
                }
            }

            for ((packageName, launches) in eventsData.launches) {
                if (launches > 0) {
                    launchCounts[packageName] = (launchCounts[packageName] ?: 0) + launches
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying usage events", e)
        }

        return aggregatedStats
            .filter { (_, usageTime) -> usageTime >= MIN_USAGE_MS }
            .map { (packageName, usageTime) ->
                AppUsageInfo(
                    packageName = packageName,
                    appName = getAppName(packageName),
                    usageTimeMillis = usageTime,
                    appIcon = getAppIcon(packageName),
                    launchCount = launchCounts[packageName] ?: 0
                )
            }
            .sortedByDescending { it.usageTimeMillis }
    }

    private fun getUsageFromEvents(startTime: Long, endTime: Long): UsageEventsData {
        val statsManager = usageStatsManager ?: return UsageEventsData(
            durations = emptyMap(),
            launches = emptyMap(),
            totalLaunches = 0,
            unlockCount = 0,
            notificationCount = 0,
            totalSessions = 0,
            shortSessions = 0
        )

        val usageMap = mutableMapOf<String, Long>()
        val launchMap = mutableMapOf<String, Int>()
        val lastResumeTime = mutableMapOf<String, Long>()

        var totalLaunches = 0
        var unlockCount = 0
        var notificationCount = 0
        var totalSessions = 0
        var shortSessions = 0

        try {
            val usageEvents = statsManager.queryEvents(startTime, endTime)
            if (usageEvents == null) {
                return UsageEventsData(
                    durations = emptyMap(),
                    launches = emptyMap(),
                    totalLaunches = 0,
                    unlockCount = 0,
                    notificationCount = 0,
                    totalSessions = 0,
                    shortSessions = 0
                )
            }

            val event = UsageEvents.Event()
            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event)
                val rawPackageName = event.packageName ?: continue
                val packageName = normalizePackageName(rawPackageName)
                if (packageName.isBlank()) continue

                when (event.eventType) {
                    UsageEvents.Event.ACTIVITY_RESUMED -> {
                        launchMap[packageName] = (launchMap[packageName] ?: 0) + 1
                        totalLaunches += 1
                        lastResumeTime[packageName] = event.timeStamp
                    }

                    @Suppress("DEPRECATION")
                    UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                            if (!lastResumeTime.containsKey(packageName)) {
                                launchMap[packageName] = (launchMap[packageName] ?: 0) + 1
                                totalLaunches += 1
                            }
                            lastResumeTime[packageName] = event.timeStamp
                        }
                    }

                    UsageEvents.Event.ACTIVITY_PAUSED -> {
                        val resumeTime = lastResumeTime.remove(packageName) ?: continue
                        val duration = event.timeStamp - resumeTime
                        if (duration > 0 && duration < 24 * 60 * 60 * 1000) {
                            usageMap[packageName] = (usageMap[packageName] ?: 0L) + duration
                            totalSessions += 1
                            if (duration <= SHORT_SESSION_MS) {
                                shortSessions += 1
                            }
                        }
                    }

                    @Suppress("DEPRECATION")
                    UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                            val resumeTime = lastResumeTime.remove(packageName) ?: continue
                            val duration = event.timeStamp - resumeTime
                            if (duration > 0 && duration < 24 * 60 * 60 * 1000) {
                                usageMap[packageName] = (usageMap[packageName] ?: 0L) + duration
                                totalSessions += 1
                                if (duration <= SHORT_SESSION_MS) {
                                    shortSessions += 1
                                }
                            }
                        }
                    }

                    UsageEvents.Event.KEYGUARD_HIDDEN -> {
                        unlockCount += 1
                    }

                    EVENT_NOTIFICATION_INTERRUPTION -> {
                        notificationCount += 1
                    }
                }
            }

            val currentTime = System.currentTimeMillis().coerceAtMost(endTime)
            for ((packageName, resumeTime) in lastResumeTime) {
                if (resumeTime > 0 && currentTime > resumeTime) {
                    val duration = currentTime - resumeTime
                    if (duration > 0 && duration < 24 * 60 * 60 * 1000) {
                        usageMap[packageName] = (usageMap[packageName] ?: 0L) + duration
                        totalSessions += 1
                        if (duration <= SHORT_SESSION_MS) {
                            shortSessions += 1
                        }
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException querying usage events", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error processing usage events", e)
        }

        return UsageEventsData(
            durations = usageMap,
            launches = launchMap,
            totalLaunches = totalLaunches,
            unlockCount = unlockCount,
            notificationCount = notificationCount,
            totalSessions = totalSessions,
            shortSessions = shortSessions
        )
    }

    private fun getAppName(packageName: String): String {
        val normalizedPackageName = normalizePackageName(packageName)

        val cachedLabel = launcherAppCache[normalizedPackageName]?.label?.trim().orEmpty()
        if (cachedLabel.isNotBlank() && !isLikelySuffixLabel(cachedLabel, normalizedPackageName)) {
            return cachedLabel
        }

        val launchLabel = getLaunchActivityLabel(normalizedPackageName)
        if (!launchLabel.isNullOrBlank() && !isLikelySuffixLabel(launchLabel, normalizedPackageName)) {
            return launchLabel
        }

        val appLabel = try {
            val appInfo = packageManager.getApplicationInfo(normalizedPackageName, 0)
            packageManager.getApplicationLabel(appInfo)?.toString()?.trim().orEmpty()
        } catch (_: PackageManager.NameNotFoundException) {
            ""
        }

        if (appLabel.isNotBlank() && !isLikelySuffixLabel(appLabel, normalizedPackageName)) {
            return appLabel
        }

        if (cachedLabel.isNotBlank()) return cachedLabel
        if (!launchLabel.isNullOrBlank()) return launchLabel
        if (appLabel.isNotBlank()) return appLabel

        return fallbackLabelFromPackage(normalizedPackageName)
    }

    private fun getLaunchActivityLabel(packageName: String): String? {
        return try {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName) ?: return null
            val resolveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.resolveActivity(launchIntent, PackageManager.ResolveInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.resolveActivity(launchIntent, 0)
            }
            resolveInfo?.loadLabel(packageManager)?.toString()?.trim()?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    private fun getAppIcon(packageName: String): Drawable? {
        val normalizedPackageName = normalizePackageName(packageName)

        launcherAppCache[normalizedPackageName]?.icon?.let { cachedIcon ->
            return cachedIcon
        }

        return try {
            packageManager.getApplicationIcon(normalizedPackageName)
        } catch (e: Exception) {
            null
        }
    }

    private fun loadLauncherAppCache(): Map<String, LauncherAppEntry> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val entries = mutableMapOf<String, LauncherAppEntry>()

        return try {
            val activities = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.queryIntentActivities(intent, 0)
            }

            activities.forEach { resolveInfo ->
                val packageName = normalizePackageName(resolveInfo.activityInfo?.packageName ?: return@forEach)
                if (packageName.isBlank()) return@forEach

                val label = resolveInfo.loadLabel(packageManager)?.toString()?.trim()
                    .takeUnless { it.isNullOrBlank() }
                    ?: fallbackLabelFromPackage(packageName)

                val icon = try {
                    resolveInfo.loadIcon(packageManager)
                } catch (_: Exception) {
                    null
                }

                entries[packageName] = LauncherAppEntry(label = label, icon = icon)
            }

            entries
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load launcher app cache", e)
            emptyMap()
        }
    }

    private fun isLikelySuffixLabel(label: String, packageName: String): Boolean {
        val normalizedLabel = label.trim().lowercase(Locale.getDefault())
        val suffix = packageName.substringAfterLast('.').lowercase(Locale.getDefault())
        return normalizedLabel == suffix || normalizedLabel == suffix.replaceFirstChar { it.uppercase() }
    }

    private fun fallbackLabelFromPackage(packageName: String): String {
        return packageName.substringAfterLast('.')
            .replaceFirstChar { it.titlecase(Locale.getDefault()) }
    }

    private fun normalizePackageName(packageName: String): String {
        return packageName.substringBefore(":").trim()
    }

    fun getDateString(daysAgo: Int = 0): String {
        val calendar = Calendar.getInstance()
        if (daysAgo > 0) {
            calendar.add(Calendar.DAY_OF_YEAR, -daysAgo)
        }
        return dateFormat.format(calendar.time)
    }

    fun getLast7DaysDates(): List<String> {
        return (0..6).map { getDateString(it) }
    }

    fun calculateTotalScreenTime(usageList: List<AppUsageInfo>): Long {
        return usageList.sumOf { it.usageTimeMillis }
    }

    fun formatScreenTime(totalMillis: Long): String {
        val hours = totalMillis / (1000 * 60 * 60)
        val minutes = (totalMillis / (1000 * 60)) % 60

        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "0m"
        }
    }
}


