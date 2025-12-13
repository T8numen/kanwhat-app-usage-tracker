package com.abhinavvaidya.appusagetracker.data.local

import android.content.Context
import com.abhinavvaidya.appusagetracker.domain.model.AppUsageInfo
import com.abhinavvaidya.appusagetracker.domain.model.DailyUsageSummary
import com.abhinavvaidya.appusagetracker.system.usage.UsageStatsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class UsageRepository(context: Context) {

    private val database = AppUsageDatabase.getDatabase(context)
    private val dao = database.appUsageDao()
    private val usageStatsHelper = UsageStatsHelper(context)

    fun hasUsagePermission(): Boolean = usageStatsHelper.hasUsagePermission()

    suspend fun refreshTodayUsage() = withContext(Dispatchers.IO) {
        val today = usageStatsHelper.getDateString()
        val usageList = usageStatsHelper.getTodayUsage()

        dao.deleteUsageForDate(today)

        val entities = usageList.map { usage ->
            AppUsageEntity(
                packageName = usage.packageName,
                appName = usage.appName,
                usageTimeMillis = usage.usageTimeMillis,
                date = today
            )
        }
        dao.insertAll(entities)
    }

    suspend fun refreshWeeklyUsage() = withContext(Dispatchers.IO) {
        for (daysAgo in 0..6) {
            val date = usageStatsHelper.getDateString(daysAgo)
            val usageList = usageStatsHelper.getUsageForDate(daysAgo)

            dao.deleteUsageForDate(date)

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

    fun getTodayUsageFlow(): Flow<List<AppUsageInfo>> {
        val today = usageStatsHelper.getDateString()
        return dao.getUsageForDate(today).map { entities ->
            entities.map { entity ->
                AppUsageInfo(
                    packageName = entity.packageName,
                    appName = entity.appName,
                    usageTimeMillis = entity.usageTimeMillis,
                    appIcon = null
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
                                appIcon = null
                            )
                        }
                    )
                }
                .sortedByDescending { it.date }
        }
    }

    fun getTodayUsageDirect(): List<AppUsageInfo> = usageStatsHelper.getTodayUsage()
}

