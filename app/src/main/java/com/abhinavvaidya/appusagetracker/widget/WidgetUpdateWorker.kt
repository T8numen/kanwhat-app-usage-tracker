package com.abhinavvaidya.appusagetracker.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.abhinavvaidya.appusagetracker.data.local.UsageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker for battery-efficient widget updates.
 *
 * BATTERY OPTIMIZATION STRATEGY:
 * - Runs at controlled intervals (30-60 minutes)
 * - Respects device idle and battery states
 * - Only updates cache and triggers widget refresh
 * - NO direct UsageStats queries in widget provider
 *
 * DESIGN DECISIONS:
 * - Uses CoroutineWorker for clean async handling
 * - Constraints ensure we don't drain battery during low power states
 * - Widget reads from Room cache, not from this worker directly
 */
class WidgetUpdateWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "WidgetUpdateWorker"
        private const val WORK_NAME = "widget_update_work"

        // Update interval: 30 minutes for balance between freshness and battery
        private const val UPDATE_INTERVAL_MINUTES = 30L
        private const val FLEX_INTERVAL_MINUTES = 15L

        /**
         * Schedules periodic widget updates using WorkManager.
         * Called when:
         * - First widget is added
         * - App starts with existing widgets
         * - Device reboots
         */
        fun schedulePeriodicUpdate(context: Context) {
            Log.d(TAG, "Scheduling periodic widget updates")

            // Constraints for battery efficiency
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)     // Don't run when battery is low
                .build()

            val workRequest = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(
                repeatInterval = UPDATE_INTERVAL_MINUTES,
                repeatIntervalTimeUnit = TimeUnit.MINUTES,
                flexTimeInterval = FLEX_INTERVAL_MINUTES,
                flexTimeIntervalUnit = TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .addTag(WORK_NAME)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,  // Don't restart if already scheduled
                workRequest
            )

            Log.d(TAG, "Widget update work scheduled (${UPDATE_INTERVAL_MINUTES}min interval)")
        }

        /**
         * Cancels periodic widget updates.
         * Called when last widget is removed.
         */
        fun cancelPeriodicUpdate(context: Context) {
            Log.d(TAG, "Canceling periodic widget updates")
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }

        /**
         * Triggers an immediate one-time widget update.
         * Used for manual refresh or after permission granted.
         */
        suspend fun triggerImmediateUpdate(context: Context) {
            Log.d(TAG, "Triggering immediate widget update")

            withContext(Dispatchers.IO) {
                try {
                    // Refresh the cache
                    val repository = UsageRepository(context)
                    repository.refreshWidgetCache()

                    // Update all Glance widgets
                    val glanceManager = GlanceAppWidgetManager(context)
                    val glanceIds = glanceManager.getGlanceIds(UsageTrackerWidget::class.java)

                    glanceIds.forEach { glanceId ->
                        UsageTrackerWidget().update(context, glanceId)
                    }

                    Log.d(TAG, "Immediate update completed for ${glanceIds.size} widgets")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to trigger immediate update", e)
                }
            }
        }
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Widget update worker starting")

        return withContext(Dispatchers.IO) {
            try {
                // Step 1: Refresh the widget cache
                val repository = UsageRepository(context)
                repository.refreshWidgetCache()

                // Step 2: Update all Glance widgets
                val glanceManager = GlanceAppWidgetManager(context)
                val glanceIds = glanceManager.getGlanceIds(UsageTrackerWidget::class.java)

                if (glanceIds.isEmpty()) {
                    Log.d(TAG, "No widgets found, skipping update")
                    // Cancel periodic work if no widgets exist
                    cancelPeriodicUpdate(context)
                    return@withContext Result.success()
                }

                glanceIds.forEach { glanceId ->
                    try {
                        UsageTrackerWidget().update(context, glanceId)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to update widget $glanceId", e)
                    }
                }

                Log.d(TAG, "Widget update completed for ${glanceIds.size} widgets")
                Result.success()
            } catch (e: Exception) {
                Log.e(TAG, "Widget update failed", e)
                // Retry on failure (WorkManager will handle backoff)
                Result.retry()
            }
        }
    }
}

