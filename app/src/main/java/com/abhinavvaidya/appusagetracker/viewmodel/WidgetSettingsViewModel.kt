package com.abhinavvaidya.appusagetracker.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.abhinavvaidya.appusagetracker.data.preferences.AppListMetric
import com.abhinavvaidya.appusagetracker.data.preferences.WidgetPreferencesRepository
import com.abhinavvaidya.appusagetracker.data.preferences.WidgetTheme
import com.abhinavvaidya.appusagetracker.widget.WidgetUpdateWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class PackageParseResult(
    val validPackages: Set<String>,
    val invalidPackages: List<String>
)

data class WidgetSettingsUiState(
    val selectedTheme: WidgetTheme = WidgetTheme.DARK_MINIMAL,
    val dailyGoalHours: Float = 4f,
    val showTopApp: Boolean = true,
    val excludeSystemApps: Boolean = true,
    val excludedPackages: Set<String> = emptySet(),
    val appListMetric: AppListMetric = AppListMetric.USAGE_TIME,
    val backgroundImageUri: String? = null,
    val isLoading: Boolean = true
)

class WidgetSettingsViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "WidgetSettingsVM"
        private val PACKAGE_REGEX = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$")

        fun parsePackageInput(rawInput: String): PackageParseResult {
            val normalized = rawInput
                .split('\n', ',', ';')
                .map { it.trim() }
                .filter { it.isNotBlank() }

            if (normalized.isEmpty()) {
                return PackageParseResult(validPackages = emptySet(), invalidPackages = emptyList())
            }

            val valid = linkedSetOf<String>()
            val invalid = mutableListOf<String>()

            normalized.forEach { pkg ->
                if (PACKAGE_REGEX.matches(pkg)) {
                    valid.add(pkg)
                } else {
                    invalid.add(pkg)
                }
            }

            return PackageParseResult(validPackages = valid, invalidPackages = invalid)
        }
    }

    private val preferencesRepository = WidgetPreferencesRepository(application)

    private val _uiState = MutableStateFlow(WidgetSettingsUiState())
    val uiState: StateFlow<WidgetSettingsUiState> = _uiState.asStateFlow()

    init {
        loadPreferences()
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            try {
                val baseFlow = combine(
                    preferencesRepository.widgetThemeFlow,
                    preferencesRepository.dailyUsageGoalFlow,
                    preferencesRepository.showTopAppFlow,
                    preferencesRepository.excludeSystemAppsFlow,
                    preferencesRepository.excludedPackagesFlow
                ) { theme, goalMs, showTopApp, excludeSystemApps, excludedPackages ->
                    WidgetSettingsUiState(
                        selectedTheme = theme,
                        dailyGoalHours = goalMs / (60 * 60 * 1000f),
                        showTopApp = showTopApp,
                        excludeSystemApps = excludeSystemApps,
                        excludedPackages = excludedPackages,
                        isLoading = false
                    )
                }

                combine(
                    baseFlow,
                    preferencesRepository.appListMetricFlow,
                    preferencesRepository.backgroundImageUriFlow
                ) { baseState, metric, backgroundUri ->
                    baseState.copy(
                        appListMetric = metric,
                        backgroundImageUri = backgroundUri
                    )
                }.collect { state ->
                    _uiState.value = state
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load preferences", e)
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun setTheme(theme: WidgetTheme) {
        viewModelScope.launch {
            preferencesRepository.setWidgetTheme(theme)
            triggerWidgetUpdate()
        }
    }

    fun setDailyGoal(hours: Float) {
        viewModelScope.launch {
            val goalMs = (hours * 60 * 60 * 1000).toLong()
            preferencesRepository.setDailyUsageGoal(goalMs)
        }
    }

    fun setShowTopApp(show: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setShowTopApp(show)
            triggerWidgetUpdate()
        }
    }

    fun setExcludeSystemApps(exclude: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setExcludeSystemApps(exclude)
            triggerWidgetUpdate()
        }
    }

    fun setExcludedPackages(packages: Set<String>) {
        viewModelScope.launch {
            preferencesRepository.setExcludedPackages(packages)
            triggerWidgetUpdate()
        }
    }

    fun setAppListMetric(metric: AppListMetric) {
        viewModelScope.launch {
            preferencesRepository.setAppListMetric(metric)
        }
    }

    fun setBackgroundImageUri(uri: String?) {
        viewModelScope.launch {
            preferencesRepository.setBackgroundImageUri(uri)
        }
    }

    private fun triggerWidgetUpdate() {
        viewModelScope.launch {
            try {
                WidgetUpdateWorker.triggerImmediateUpdate(getApplication())
            } catch (e: Exception) {
                Log.e(TAG, "Failed to trigger widget update", e)
            }
        }
    }
}
