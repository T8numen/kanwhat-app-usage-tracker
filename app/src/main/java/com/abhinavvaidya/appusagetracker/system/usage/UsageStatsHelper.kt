package com.abhinavvaidya.appusagetracker.system.usage

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Process
import android.util.Log
import com.abhinavvaidya.appusagetracker.domain.model.AppUsageInfo
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Helper class for fetching app usage statistics from Android's UsageStatsManager.
 *
 * IMPORTANT NOTES:
 * 1. This class requires PACKAGE_USAGE_STATS permission to be granted via Settings
 * 2. Permission MUST be checked before any query to avoid empty/incorrect results
 * 3. Time windows must be calculated correctly (start of day to current time for today)
 * 4. Some OEMs (Samsung, MIUI) may have delayed data availability after permission grant
 * 5. UsageStats aggregates data, so we use INTERVAL_DAILY for daily stats
 */
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

    companion object {
        private const val TAG = "UsageStatsHelper"
        // Minimum usage threshold (30 seconds) to filter out brief app launches
        private const val MIN_USAGE_MS = 30_000L
    }

    /**
     * Checks if the app has Usage Access permission using AppOpsManager.
     * This MUST be called before any usage queries.
     *
     * @return true if permission is granted, false otherwise
     */
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

            val hasPermission = mode == AppOpsManager.MODE_ALLOWED
            Log.d(TAG, "Usage permission check: $hasPermission (mode=$mode)")
            hasPermission
        } catch (e: Exception) {
            Log.e(TAG, "Error checking usage permission", e)
            false
        }
    }

    /**
     * Fetches today's app usage data.
     *
     * Time window: Start of today (00:00:00.000) to current time
     * This ensures we capture all usage for the current day.
     *
     * @return List of AppUsageInfo sorted by usage time (descending), empty list if no permission
     */
    fun getTodayUsage(): List<AppUsageInfo> {
        // CRITICAL: Always check permission before querying
        if (!hasUsagePermission()) {
            Log.w(TAG, "getTodayUsage: No usage permission, returning empty list")
            return emptyList()
        }

        // Calculate start of today at midnight
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        Log.d(TAG, "getTodayUsage: Querying from ${formatTimestamp(startTime)} to ${formatTimestamp(endTime)}")

        return getUsageForTimeRange(startTime, endTime, isToday = true)
    }

    /**
     * Fetches app usage data for a specific day.
     *
     * @param daysAgo Number of days in the past (0 = today, 1 = yesterday, etc.)
     * @return List of AppUsageInfo sorted by usage time (descending), empty list if no permission
     */
    fun getUsageForDate(daysAgo: Int): List<AppUsageInfo> {
        // CRITICAL: Always check permission before querying
        if (!hasUsagePermission()) {
            Log.w(TAG, "getUsageForDate: No usage permission, returning empty list")
            return emptyList()
        }

        val calendar = Calendar.getInstance()

        // Set to the target date
        calendar.add(Calendar.DAY_OF_YEAR, -daysAgo)

        // Start of day: 00:00:00.000
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis

        // End of day: for today use current time, for past days use 23:59:59.999
        val endTime = if (daysAgo == 0) {
            System.currentTimeMillis()
        } else {
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            calendar.timeInMillis
        }

        Log.d(TAG, "getUsageForDate($daysAgo): Querying from ${formatTimestamp(startTime)} to ${formatTimestamp(endTime)}")

        return getUsageForTimeRange(startTime, endTime, isToday = daysAgo == 0)
    }

    /**
     * Core method to fetch usage statistics for a given time range.
     *
     * FIXED ISSUES:
     * 1. Now properly checks permission before querying (prevents empty results)
     * 2. Uses INTERVAL_DAILY as primary source (most reliable across devices)
     * 3. Uses UsageEvents for today's data to capture real-time foreground usage
     * 4. Properly aggregates data by package name
     * 5. Handles OEM edge cases with proper null checks
     *
     * @param startTime Start of time range in milliseconds
     * @param endTime End of time range in milliseconds
     * @param isToday Whether this query is for today (enables UsageEvents for real-time data)
     * @return List of AppUsageInfo sorted by usage time (descending)
     */
    private fun getUsageForTimeRange(startTime: Long, endTime: Long, isToday: Boolean = false): List<AppUsageInfo> {
        val statsManager = usageStatsManager
        if (statsManager == null) {
            Log.e(TAG, "UsageStatsManager is null, cannot query usage stats")
            return emptyList()
        }

        val aggregatedStats = mutableMapOf<String, Long>()

        // PRIMARY METHOD: Query usage stats with INTERVAL_DAILY
        // This is the most reliable method across different Android versions and OEMs
        try {
            val dailyStats = statsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
            )

            if (dailyStats.isNullOrEmpty()) {
                Log.d(TAG, "INTERVAL_DAILY returned no stats")
            } else {
                Log.d(TAG, "INTERVAL_DAILY returned ${dailyStats.size} entries")

                for (stats in dailyStats) {
                    val packageName = stats.packageName
                    val totalTime = stats.totalTimeInForeground

                    if (totalTime > 0) {
                        // Aggregate usage per package (same package can appear multiple times)
                        val current = aggregatedStats[packageName] ?: 0L
                        aggregatedStats[packageName] = current + totalTime
                        Log.v(TAG, "Package: $packageName, Time: ${totalTime}ms, Accumulated: ${aggregatedStats[packageName]}ms")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying INTERVAL_DAILY usage stats", e)
        }

        // SECONDARY METHOD (for today only): Use UsageEvents for real-time accuracy
        // UsageEvents provides more accurate foreground time for current-day tracking
        // because it captures the actual ACTIVITY_RESUMED and ACTIVITY_PAUSED events
        if (isToday) {
            try {
                val eventsStats = getUsageFromEvents(startTime, endTime)
                Log.d(TAG, "UsageEvents returned ${eventsStats.size} entries")

                for ((packageName, time) in eventsStats) {
                    if (time > 0) {
                        // Take the maximum of events-based and stats-based time
                        // Events are more accurate for today's data
                        val current = aggregatedStats[packageName] ?: 0L
                        val newValue = maxOf(current, time)
                        if (newValue != current) {
                            Log.v(TAG, "Using events time for $packageName: ${time}ms (was ${current}ms)")
                        }
                        aggregatedStats[packageName] = newValue
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error querying usage events", e)
            }
        }

        Log.d(TAG, "Total unique packages with usage: ${aggregatedStats.size}")

        // Filter and transform results
        val result = aggregatedStats
            .filter { (packageName, usageTime) ->
                // Filter out apps with usage below threshold
                val passesMinimum = usageTime >= MIN_USAGE_MS
                if (!passesMinimum) {
                    Log.v(TAG, "Filtered $packageName: usage ${usageTime}ms below minimum $MIN_USAGE_MS")
                }
                passesMinimum
            }
            .filter { (packageName, _) ->
                // Filter out system packages that are not user-facing
                val isSystem = isSystemApp(packageName)
                if (isSystem) {
                    Log.v(TAG, "Filtered $packageName: system app")
                }
                !isSystem
            }
            .map { (packageName, usageTime) ->
                AppUsageInfo(
                    packageName = packageName,
                    appName = getAppName(packageName),
                    usageTimeMillis = usageTime,
                    appIcon = null // Icons loaded in UI layer for performance
                )
            }
            .sortedByDescending { it.usageTimeMillis }

        Log.d(TAG, "Final result: ${result.size} apps")
        result.take(5).forEach { app ->
            Log.d(TAG, "  ${app.appName}: ${app.usageTimeMillis / 1000 / 60}m ${(app.usageTimeMillis / 1000) % 60}s")
        }

        return result
    }

    /**
     * Calculates usage time from UsageEvents.
     *
     * This method iterates through usage events and calculates foreground time by
     * tracking ACTIVITY_RESUMED and ACTIVITY_PAUSED event pairs.
     *
     * BENEFITS over queryUsageStats:
     * - More accurate for current-day data
     * - Captures apps currently in foreground
     * - Real-time usage tracking
     *
     * @param startTime Start of time range in milliseconds
     * @param endTime End of time range in milliseconds
     * @return Map of package name to usage time in milliseconds
     */
    private fun getUsageFromEvents(startTime: Long, endTime: Long): Map<String, Long> {
        val statsManager = usageStatsManager ?: return emptyMap()

        val usageMap = mutableMapOf<String, Long>()
        val lastResumeTime = mutableMapOf<String, Long>()

        try {
            val usageEvents = statsManager.queryEvents(startTime, endTime)
            if (usageEvents == null) {
                Log.d(TAG, "queryEvents returned null")
                return emptyMap()
            }

            val event = UsageEvents.Event()
            var eventCount = 0

            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event)
                eventCount++
                val packageName = event.packageName ?: continue

                when (event.eventType) {
                    // App moved to foreground
                    UsageEvents.Event.ACTIVITY_RESUMED -> {
                        lastResumeTime[packageName] = event.timeStamp
                    }
                    // Deprecated but still used on older devices
                    @Suppress("DEPRECATION")
                    UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                            lastResumeTime[packageName] = event.timeStamp
                        }
                    }
                    // App moved to background
                    UsageEvents.Event.ACTIVITY_PAUSED -> {
                        val resumeTime = lastResumeTime.remove(packageName)
                        if (resumeTime != null && resumeTime > 0) {
                            val duration = event.timeStamp - resumeTime
                            if (duration > 0 && duration < 24 * 60 * 60 * 1000) { // Sanity check: < 24 hours
                                usageMap[packageName] = (usageMap[packageName] ?: 0L) + duration
                            }
                        }
                    }
                    // Deprecated but still used on older devices
                    @Suppress("DEPRECATION")
                    UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                            val resumeTime = lastResumeTime.remove(packageName)
                            if (resumeTime != null && resumeTime > 0) {
                                val duration = event.timeStamp - resumeTime
                                if (duration > 0 && duration < 24 * 60 * 60 * 1000) {
                                    usageMap[packageName] = (usageMap[packageName] ?: 0L) + duration
                                }
                            }
                        }
                    }
                }
            }

            Log.d(TAG, "Processed $eventCount usage events")

            // Account for apps that are still in foreground (no PAUSED event yet)
            val currentTime = System.currentTimeMillis().coerceAtMost(endTime)
            for ((packageName, resumeTime) in lastResumeTime) {
                if (resumeTime > 0 && currentTime > resumeTime) {
                    val duration = currentTime - resumeTime
                    if (duration > 0 && duration < 24 * 60 * 60 * 1000) {
                        usageMap[packageName] = (usageMap[packageName] ?: 0L) + duration
                        Log.d(TAG, "App $packageName still in foreground, adding ${duration}ms")
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException querying events - permission may have been revoked", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error processing usage events", e)
        }

        return usageMap
    }

    /**
     * Helper method to format timestamp for logging
     */
    private fun formatTimestamp(timestamp: Long): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(timestamp)
    }

    /**
     * Determines if a package is a system component that should be filtered out.
     *
     * IMPORTANT: We only filter out core system components, NOT user-facing system apps.
     * Examples of what we KEEP:
     * - Play Store (com.android.vending)
     * - Google apps (com.google.*)
     * - Pre-installed apps that users actually interact with
     *
     * Examples of what we FILTER:
     * - System UI (com.android.systemui)
     * - Core system processes (android)
     * - Input methods (when not actively used by user)
     *
     * @param packageName Package name to check
     * @return true if the package should be filtered out
     */
    private fun isSystemApp(packageName: String): Boolean {
        // Whitelist: These should NEVER be filtered even if they're system apps
        val whitelistPrefixes = listOf(
            "com.google.android.apps.", // Google apps (Gmail, Maps, etc.)
            "com.google.android.youtube",
            "com.android.chrome",
            "com.android.vending", // Play Store
        )

        if (whitelistPrefixes.any { packageName.startsWith(it) }) {
            return false
        }

        // Blacklist: Core system components that should always be filtered
        val blacklistPrefixes = listOf(
            "com.android.systemui",
            "com.android.settings",
            "com.android.phone",
            "com.android.incallui",
            "com.android.server",
            "com.android.providers.",
            "com.android.inputmethod",
            "com.google.android.inputmethod",
            "com.android.internal",
            "com.android.keyguard",
            "com.android.launcher", // Default launcher
            "com.android.packageinstaller",
            "com.android.permissioncontroller",
        )

        val blacklistExact = listOf(
            "android",
            "com.android.shell",
        )

        if (blacklistExact.contains(packageName) ||
            blacklistPrefixes.any { packageName.startsWith(it) }) {
            return true
        }

        // For other apps, check if they're pure system components
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val isUpdatedSystemApp = (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0

            // Only filter pure system apps that haven't been updated by the user
            // Updated system apps are likely user-facing (e.g., pre-installed apps that got updates)
            isSystemApp && !isUpdatedSystemApp
        } catch (e: PackageManager.NameNotFoundException) {
            // If we can't get app info, it might be uninstalled - filter it
            Log.v(TAG, "Package not found: $packageName")
            true
        }
    }

    /**
     * Gets the display name for a package.
     *
     * @param packageName Package name to look up
     * @return Application label or fallback to simplified package name
     */
    private fun getAppName(packageName: String): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            // Return a cleaned-up package name as fallback
            packageName.substringAfterLast(".")
                .replaceFirstChar { it.uppercase() }
        }
    }

    /**
     * Gets the icon for a package.
     * Note: Icons are loaded lazily in the UI layer for better performance.
     *
     * @param packageName Package name to look up
     * @return Application icon drawable or null
     */
    private fun getAppIcon(packageName: String): Drawable? {
        return try {
            packageManager.getApplicationIcon(packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    /**
     * Gets the date string for a given number of days ago.
     *
     * @param daysAgo Number of days in the past (0 = today)
     * @return Date string in yyyy-MM-dd format
     */
    fun getDateString(daysAgo: Int = 0): String {
        val calendar = Calendar.getInstance()
        if (daysAgo > 0) {
            calendar.add(Calendar.DAY_OF_YEAR, -daysAgo)
        }
        return dateFormat.format(calendar.time)
    }

    /**
     * Gets a list of date strings for the last 7 days.
     *
     * @return List of date strings in yyyy-MM-dd format, most recent first
     */
    fun getLast7DaysDates(): List<String> {
        return (0..6).map { getDateString(it) }
    }

    /**
     * Calculates total screen time from a list of app usage info.
     * This is a utility method to ensure consistent calculation.
     *
     * @param usageList List of AppUsageInfo
     * @return Total usage time in milliseconds
     */
    fun calculateTotalScreenTime(usageList: List<AppUsageInfo>): Long {
        return usageList.sumOf { it.usageTimeMillis }
    }

    /**
     * Formats total screen time as a human-readable string.
     *
     * @param totalMillis Total time in milliseconds
     * @return Formatted string like "3h 45m"
     */
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

