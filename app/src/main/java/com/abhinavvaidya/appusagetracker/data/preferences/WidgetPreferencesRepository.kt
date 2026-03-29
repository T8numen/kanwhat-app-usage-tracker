package com.abhinavvaidya.appusagetracker.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.widgetDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "widget_preferences"
)

enum class WidgetTheme(val displayName: String, val description: String) {
    DARK_MINIMAL("Dark Minimal", "Clean, dark theme with subtle accents"),
    NEON_CYBER("Neon Cyber", "Vibrant neon colors with cyber aesthetic"),
    SOFT_PASTEL("Soft Pastel", "Calm, pastel colors for relaxed viewing")
}

enum class AppListMetric {
    USAGE_TIME,
    LAUNCH_COUNT,
    USAGE_PERCENTAGE
}

class WidgetPreferencesRepository(private val context: Context) {

    companion object {
        private val WIDGET_THEME_KEY = intPreferencesKey("widget_theme")
        private val DAILY_USAGE_GOAL_KEY = longPreferencesKey("daily_usage_goal")
        private val SHOW_TOP_APP_KEY = intPreferencesKey("show_top_app")
        private val EXCLUDE_SYSTEM_APPS_KEY = intPreferencesKey("exclude_system_apps")
        private val EXCLUDED_PACKAGES_KEY = stringSetPreferencesKey("excluded_packages")
        private val APP_LIST_METRIC_KEY = intPreferencesKey("app_list_metric")
        private val BACKGROUND_IMAGE_URI_KEY = stringPreferencesKey("background_image_uri")

        private const val DEFAULT_DAILY_GOAL_MS = 4 * 60 * 60 * 1000L
    }

    private val dataStore = context.widgetDataStore

    val widgetThemeFlow: Flow<WidgetTheme> = dataStore.data.map { preferences ->
        val themeOrdinal = preferences[WIDGET_THEME_KEY] ?: WidgetTheme.DARK_MINIMAL.ordinal
        WidgetTheme.entries.getOrElse(themeOrdinal) { WidgetTheme.DARK_MINIMAL }
    }

    val dailyUsageGoalFlow: Flow<Long> = dataStore.data.map { preferences ->
        preferences[DAILY_USAGE_GOAL_KEY] ?: DEFAULT_DAILY_GOAL_MS
    }

    val showTopAppFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        (preferences[SHOW_TOP_APP_KEY] ?: 1) == 1
    }

    val excludeSystemAppsFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        (preferences[EXCLUDE_SYSTEM_APPS_KEY] ?: 1) == 1
    }

    val excludedPackagesFlow: Flow<Set<String>> = dataStore.data.map { preferences ->
        (preferences[EXCLUDED_PACKAGES_KEY] ?: emptySet())
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSortedSet()
    }

    val appListMetricFlow: Flow<AppListMetric> = dataStore.data.map { preferences ->
        val metricOrdinal = preferences[APP_LIST_METRIC_KEY] ?: AppListMetric.USAGE_TIME.ordinal
        AppListMetric.entries.getOrElse(metricOrdinal) { AppListMetric.USAGE_TIME }
    }

    val backgroundImageUriFlow: Flow<String?> = dataStore.data.map { preferences ->
        preferences[BACKGROUND_IMAGE_URI_KEY]?.takeIf { it.isNotBlank() }
    }

    suspend fun getWidgetTheme(): WidgetTheme = widgetThemeFlow.first()

    suspend fun getDailyUsageGoal(): Long = dailyUsageGoalFlow.first()

    suspend fun getShowTopApp(): Boolean = showTopAppFlow.first()

    suspend fun getExcludeSystemApps(): Boolean = excludeSystemAppsFlow.first()

    suspend fun getExcludedPackages(): Set<String> = excludedPackagesFlow.first()

    suspend fun getAppListMetric(): AppListMetric = appListMetricFlow.first()

    suspend fun getBackgroundImageUri(): String? = backgroundImageUriFlow.first()

    suspend fun setWidgetTheme(theme: WidgetTheme) {
        dataStore.edit { preferences ->
            preferences[WIDGET_THEME_KEY] = theme.ordinal
        }
    }

    suspend fun setDailyUsageGoal(goalMs: Long) {
        dataStore.edit { preferences ->
            preferences[DAILY_USAGE_GOAL_KEY] = goalMs
        }
    }

    suspend fun setShowTopApp(show: Boolean) {
        dataStore.edit { preferences ->
            preferences[SHOW_TOP_APP_KEY] = if (show) 1 else 0
        }
    }

    suspend fun setExcludeSystemApps(exclude: Boolean) {
        dataStore.edit { preferences ->
            preferences[EXCLUDE_SYSTEM_APPS_KEY] = if (exclude) 1 else 0
        }
    }

    suspend fun setExcludedPackages(packages: Set<String>) {
        val normalized = packages
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSortedSet()

        dataStore.edit { preferences ->
            preferences[EXCLUDED_PACKAGES_KEY] = normalized
        }
    }

    suspend fun setAppListMetric(metric: AppListMetric) {
        dataStore.edit { preferences ->
            preferences[APP_LIST_METRIC_KEY] = metric.ordinal
        }
    }

    suspend fun setBackgroundImageUri(uri: String?) {
        dataStore.edit { preferences ->
            if (uri.isNullOrBlank()) {
                preferences.remove(BACKGROUND_IMAGE_URI_KEY)
            } else {
                preferences[BACKGROUND_IMAGE_URI_KEY] = uri
            }
        }
    }
}
