package com.abhinavvaidya.appusagetracker.system.usage

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Process
import com.abhinavvaidya.appusagetracker.domain.model.AppUsageInfo
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class UsageStatsHelper(private val context: Context) {

    private val usageStatsManager: UsageStatsManager by lazy {
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    }

    private val packageManager: PackageManager by lazy {
        context.packageManager
    }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun hasUsagePermission(): Boolean {
        val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
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
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun getTodayUsage(): List<AppUsageInfo> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        return getUsageForTimeRange(startTime, endTime)
    }

    fun getUsageForDate(daysAgo: Int): List<AppUsageInfo> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -daysAgo)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val startTime = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)

        val endTime = calendar.timeInMillis

        return getUsageForTimeRange(startTime, endTime)
    }

    private fun getUsageForTimeRange(startTime: Long, endTime: Long): List<AppUsageInfo> {
        val usageStatsList = try {
            usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
            )
        } catch (e: Exception) {
            emptyList()
        }

        if (usageStatsList.isNullOrEmpty()) {
            return emptyList()
        }

        val aggregatedStats = mutableMapOf<String, Long>()

        for (stats in usageStatsList) {
            val totalTime = stats.totalTimeInForeground
            if (totalTime > 0) {
                aggregatedStats[stats.packageName] =
                    (aggregatedStats[stats.packageName] ?: 0L) + totalTime
            }
        }

        return aggregatedStats
            .filter { it.value >= 60_000 } // At least 1 minute
            .filter { !isSystemApp(it.key) }
            .map { (packageName, usageTime) ->
                AppUsageInfo(
                    packageName = packageName,
                    appName = getAppName(packageName),
                    usageTimeMillis = usageTime,
                    appIcon = getAppIcon(packageName)
                )
            }
            .sortedByDescending { it.usageTimeMillis }
    }

    private fun isSystemApp(packageName: String): Boolean {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val isUpdatedSystem = (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0

            // Filter out pure system apps but allow user-facing apps
            val excludePackages = listOf(
                "com.android.systemui",
                "com.android.settings",
                "com.android.phone",
                "com.android.launcher",
                "com.android.providers",
                "com.android.inputmethod",
                "com.google.android.inputmethod"
            )

            val shouldExclude = excludePackages.any { packageName.startsWith(it) }
            isSystem && !isUpdatedSystem && shouldExclude
        } catch (e: PackageManager.NameNotFoundException) {
            true
        }
    }

    private fun getAppName(packageName: String): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName.substringAfterLast(".")
        }
    }

    private fun getAppIcon(packageName: String): Drawable? {
        return try {
            packageManager.getApplicationIcon(packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    fun getDateString(daysAgo: Int = 0): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -daysAgo)
        return dateFormat.format(calendar.time)
    }

    fun getLast7DaysDates(): List<String> {
        return (0..6).map { getDateString(it) }
    }
}

