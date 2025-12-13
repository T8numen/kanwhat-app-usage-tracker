package com.abhinavvaidya.appusagetracker.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// Extension property for DataStore
private val Context.widgetDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "widget_preferences"
)

/**
 * Widget theme options - Premium quality themes
 */
enum class WidgetTheme(val displayName: String, val description: String) {
    DARK_MINIMAL("Dark Minimal", "Clean, dark theme with subtle accents"),
    NEON_CYBER("Neon Cyber", "Vibrant neon colors with cyber aesthetic"),
    SOFT_PASTEL("Soft Pastel", "Calm, pastel colors for relaxed viewing")
}

/**
 * Repository for widget preferences using DataStore.
 *
 * DESIGN DECISIONS:
 * - Uses DataStore instead of SharedPreferences for type safety and async support
 * - Stores theme per widget ID for multi-widget support
 * - Includes daily usage goal for gamification
 */
class WidgetPreferencesRepository(private val context: Context) {

    companion object {
        // Preference keys
        private val WIDGET_THEME_KEY = intPreferencesKey("widget_theme")
        private val DAILY_USAGE_GOAL_KEY = longPreferencesKey("daily_usage_goal")
        private val SHOW_TOP_APP_KEY = intPreferencesKey("show_top_app") // 1 = true, 0 = false

        // Default values
        private const val DEFAULT_DAILY_GOAL_MS = 4 * 60 * 60 * 1000L // 4 hours
    }

    private val dataStore = context.widgetDataStore

    /**
     * Gets the current widget theme preference.
     */
    val widgetThemeFlow: Flow<WidgetTheme> = dataStore.data.map { preferences ->
        val themeOrdinal = preferences[WIDGET_THEME_KEY] ?: WidgetTheme.DARK_MINIMAL.ordinal
        WidgetTheme.entries.getOrElse(themeOrdinal) { WidgetTheme.DARK_MINIMAL }
    }

    /**
     * Gets the daily usage goal in milliseconds.
     */
    val dailyUsageGoalFlow: Flow<Long> = dataStore.data.map { preferences ->
        preferences[DAILY_USAGE_GOAL_KEY] ?: DEFAULT_DAILY_GOAL_MS
    }

    /**
     * Gets whether to show the top app in the widget.
     */
    val showTopAppFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        (preferences[SHOW_TOP_APP_KEY] ?: 1) == 1
    }

    /**
     * Gets the current theme synchronously (for widget provider).
     */
    suspend fun getWidgetTheme(): WidgetTheme {
        return widgetThemeFlow.first()
    }

    /**
     * Gets the daily usage goal synchronously.
     */
    suspend fun getDailyUsageGoal(): Long {
        return dailyUsageGoalFlow.first()
    }

    /**
     * Gets show top app preference synchronously.
     */
    suspend fun getShowTopApp(): Boolean {
        return showTopAppFlow.first()
    }

    /**
     * Sets the widget theme preference.
     */
    suspend fun setWidgetTheme(theme: WidgetTheme) {
        dataStore.edit { preferences ->
            preferences[WIDGET_THEME_KEY] = theme.ordinal
        }
    }

    /**
     * Sets the daily usage goal in milliseconds.
     */
    suspend fun setDailyUsageGoal(goalMs: Long) {
        dataStore.edit { preferences ->
            preferences[DAILY_USAGE_GOAL_KEY] = goalMs
        }
    }

    /**
     * Sets whether to show the top app in the widget.
     */
    suspend fun setShowTopApp(show: Boolean) {
        dataStore.edit { preferences ->
            preferences[SHOW_TOP_APP_KEY] = if (show) 1 else 0
        }
    }
}

