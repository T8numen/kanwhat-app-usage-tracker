package com.abhinavvaidya.appusagetracker.widget

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.abhinavvaidya.appusagetracker.MainActivity
import com.abhinavvaidya.appusagetracker.R
import com.abhinavvaidya.appusagetracker.data.local.UsageRepository
import com.abhinavvaidya.appusagetracker.data.local.WidgetCacheEntity
import com.abhinavvaidya.appusagetracker.data.preferences.WidgetPreferencesRepository
import com.abhinavvaidya.appusagetracker.data.preferences.WidgetTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Premium 2×2 App Widget using Jetpack Glance (Compose for widgets).
 *
 * FEATURES:
 * - Shows today's total screen time prominently
 * - Displays top app usage (optional)
 * - Progress ring with gamified color coding
 * - Multiple themes: Dark Minimal, Neon Cyber, Soft Pastel
 * - Last updated timestamp for transparency
 *
 * BATTERY OPTIMIZATION:
 * - NEVER queries UsageStats directly
 * - Reads ONLY from Room database cache
 * - Updates triggered by WorkManager at controlled intervals
 * - Tapping opens the main app for fresh data
 */
class UsageTrackerWidget : GlanceAppWidget() {

    companion object {
        private const val TAG = "UsageTrackerWidget"
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        Log.d(TAG, "Providing Glance content for widget $id")

        // Load data from cache (NEVER from UsageStats)
        val widgetData = withContext(Dispatchers.IO) {
            loadWidgetData(context)
        }

        provideContent {
            GlanceTheme {
                WidgetContent(widgetData)
            }
        }
    }

    /**
     * Loads widget data from cache only.
     * This ensures we never drain battery in the widget provider.
     */
    private suspend fun loadWidgetData(context: Context): WidgetDisplayData {
        return try {
            val repository = UsageRepository(context)
            val prefsRepository = WidgetPreferencesRepository(context)

            val cache = repository.getWidgetCache()
            val theme = prefsRepository.getWidgetTheme()
            val dailyGoal = prefsRepository.getDailyUsageGoal()
            val showTopApp = prefsRepository.getShowTopApp()

            if (cache != null) {
                WidgetDisplayData(
                    totalTimeFormatted = formatDuration(cache.totalScreenTimeMillis),
                    totalTimeMillis = cache.totalScreenTimeMillis,
                    topAppName = if (showTopApp) cache.topAppName else null,
                    topAppUsageFormatted = if (showTopApp) formatDuration(cache.topAppUsageMillis) else null,
                    lastUpdated = formatLastUpdated(cache.lastUpdatedTimestamp),
                    usageLevel = cache.usageLevel,
                    progressPercent = calculateProgress(cache.totalScreenTimeMillis, dailyGoal),
                    theme = theme,
                    hasData = true
                )
            } else {
                WidgetDisplayData(
                    totalTimeFormatted = "0h 0m",
                    totalTimeMillis = 0L,
                    topAppName = null,
                    topAppUsageFormatted = null,
                    lastUpdated = "Not yet updated",
                    usageLevel = 0,
                    progressPercent = 0f,
                    theme = theme,
                    hasData = false
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading widget data", e)
            WidgetDisplayData(
                totalTimeFormatted = "Error",
                totalTimeMillis = 0L,
                topAppName = null,
                topAppUsageFormatted = null,
                lastUpdated = "Tap to refresh",
                usageLevel = 0,
                progressPercent = 0f,
                theme = WidgetTheme.DARK_MINIMAL,
                hasData = false
            )
        }
    }

    private fun formatDuration(millis: Long): String {
        val hours = millis / (1000 * 60 * 60)
        val minutes = (millis / (1000 * 60)) % 60
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "0m"
        }
    }

    private fun formatLastUpdated(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return "Updated ${sdf.format(Date(timestamp))}"
    }

    private fun calculateProgress(usageMillis: Long, goalMillis: Long): Float {
        return (usageMillis.toFloat() / goalMillis.toFloat()).coerceIn(0f, 1f)
    }
}

/**
 * Data class holding all information needed to render the widget.
 */
data class WidgetDisplayData(
    val totalTimeFormatted: String,
    val totalTimeMillis: Long,
    val topAppName: String?,
    val topAppUsageFormatted: String?,
    val lastUpdated: String,
    val usageLevel: Int,           // 0=Low(green), 1=Medium(yellow), 2=High(red)
    val progressPercent: Float,     // 0.0 to 1.0
    val theme: WidgetTheme,
    val hasData: Boolean
)

/**
 * Main widget content composable.
 */
@Composable
private fun WidgetContent(data: WidgetDisplayData) {
    val colors = getThemeColors(data.theme, data.usageLevel)

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(colors.background)
            .cornerRadius(16.dp)
            .clickable(actionStartActivity<MainActivity>())
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Progress indicator with screen time
            TimeWithProgressBar(
                timeFormatted = data.totalTimeFormatted,
                progress = data.progressPercent,
                colors = colors
            )

            Spacer(modifier = GlanceModifier.height(8.dp))

            // Label
            Text(
                text = "Screen Time",
                style = TextStyle(
                    color = colors.subtitle,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            )

            // Top app (if available and enabled)
            if (data.topAppName != null && data.topAppUsageFormatted != null) {
                Spacer(modifier = GlanceModifier.height(4.dp))
                Text(
                    text = "${data.topAppName}: ${data.topAppUsageFormatted}",
                    style = TextStyle(
                        color = colors.topApp,
                        fontSize = 9.sp
                    ),
                    maxLines = 1
                )
            }

            // Last updated timestamp
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = data.lastUpdated,
                style = TextStyle(
                    color = colors.timestamp,
                    fontSize = 8.sp
                )
            )
        }
    }
}

/**
 * Time display with linear progress bar.
 */
@Composable
private fun TimeWithProgressBar(
    timeFormatted: String,
    progress: Float,
    colors: ThemeColors
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = GlanceModifier.fillMaxWidth()
    ) {
        Text(
            text = timeFormatted,
            style = TextStyle(
                color = colors.primaryText,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        )

        Spacer(modifier = GlanceModifier.height(8.dp))

        LinearProgressIndicator(
            progress = progress,
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(8.dp)
                .cornerRadius(4.dp),
            color = colors.progressFill,
            backgroundColor = colors.progressTrack
        )
    }
}

/**
 * Theme colors for the widget.
 */
data class ThemeColors(
    val background: ColorProvider,
    val primaryText: ColorProvider,
    val subtitle: ColorProvider,
    val topApp: ColorProvider,
    val timestamp: ColorProvider,
    val progressTrack: ColorProvider,
    val progressFill: ColorProvider
)

/**
 * Gets theme colors based on selected theme and usage level.
 *
 * GAMIFICATION:
 * - Progress ring color changes based on usage level
 * - Low usage (green) = under 2 hours
 * - Medium usage (yellow) = 2-4 hours
 * - High usage (red) = over 4 hours
 */
private fun getThemeColors(theme: WidgetTheme, usageLevel: Int): ThemeColors {
    // Usage level colors (gamification) - map to color resources
    val levelColorRes = when (theme) {
        WidgetTheme.DARK_MINIMAL -> when (usageLevel) {
            0 -> R.color.widget_usage_low
            1 -> R.color.widget_usage_medium
            else -> R.color.widget_usage_high
        }
        WidgetTheme.NEON_CYBER -> when (usageLevel) {
            0 -> R.color.widget_neon_green
            1 -> R.color.widget_neon_yellow
            else -> R.color.widget_neon_pink
        }
        WidgetTheme.SOFT_PASTEL -> when (usageLevel) {
            0 -> R.color.widget_pastel_green
            1 -> R.color.widget_pastel_yellow
            else -> R.color.widget_pastel_red
        }
    }
    return when (theme) {
        WidgetTheme.DARK_MINIMAL -> ThemeColors(
            background = ColorProvider(R.color.widget_background_dark),
            primaryText = ColorProvider(R.color.widget_text_primary),
            subtitle = ColorProvider(R.color.widget_text_secondary),
            topApp = ColorProvider(R.color.widget_text_tertiary),
            timestamp = ColorProvider(R.color.widget_text_tertiary),
            progressTrack = ColorProvider(R.color.widget_progress_track),
            progressFill = ColorProvider(levelColorRes)
        )
        WidgetTheme.NEON_CYBER -> ThemeColors(
            background = ColorProvider(R.color.widget_neon_background),
            primaryText = ColorProvider(R.color.widget_neon_cyan),
            subtitle = ColorProvider(R.color.widget_neon_magenta),
            topApp = ColorProvider(R.color.widget_neon_green),
            timestamp = ColorProvider(R.color.widget_neon_purple),
            progressTrack = ColorProvider(R.color.widget_neon_background),
            progressFill = ColorProvider(levelColorRes)
        )
        WidgetTheme.SOFT_PASTEL -> ThemeColors(
            background = ColorProvider(R.color.widget_pastel_background),
            primaryText = ColorProvider(R.color.widget_pastel_text),
            subtitle = ColorProvider(R.color.widget_pastel_text),
            topApp = ColorProvider(R.color.widget_pastel_text),
            timestamp = ColorProvider(R.color.widget_pastel_text),
            progressTrack = ColorProvider(R.color.widget_pastel_background),
            progressFill = ColorProvider(levelColorRes)
        )
    }
}

/**
 * GlanceAppWidgetReceiver for the widget.
 * This is the entry point registered in the manifest.
 */
class UsageTrackerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = UsageTrackerWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        Log.d("UsageTrackerWidget", "Widget enabled, scheduling updates")

        // Schedule periodic updates when first widget is added
        WidgetUpdateWorker.schedulePeriodicUpdate(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        Log.d("UsageTrackerWidget", "All widgets removed, canceling updates")

        // Cancel periodic updates when last widget is removed
        WidgetUpdateWorker.cancelPeriodicUpdate(context)
    }
}
