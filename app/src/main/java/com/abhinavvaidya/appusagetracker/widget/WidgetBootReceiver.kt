package com.abhinavvaidya.appusagetracker.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Boot receiver to reschedule widget updates after device reboot.
 *
 * BATTERY OPTIMIZATION:
 * - Only schedules work if widgets exist
 * - Uses WorkManager which respects Doze mode
 * - Does minimal work in onReceive
 */
class WidgetBootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "WidgetBootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Device booted, checking for widgets")

            // Check if any widgets exist and reschedule updates
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val glanceManager = GlanceAppWidgetManager(context)
                    val glanceIds = glanceManager.getGlanceIds(UsageTrackerWidget::class.java)

                    if (glanceIds.isNotEmpty()) {
                        Log.d(TAG, "Found ${glanceIds.size} widgets, scheduling updates")
                        WidgetUpdateWorker.schedulePeriodicUpdate(context)

                        // Also trigger an immediate update for fresh data
                        WidgetUpdateWorker.triggerImmediateUpdate(context)
                    } else {
                        Log.d(TAG, "No widgets found, skipping schedule")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling boot completed", e)
                }
            }
        }
    }
}

