package com.abhinavvaidya.appusagetracker.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.abhinavvaidya.appusagetracker.data.preferences.WidgetPreferencesRepository
import com.abhinavvaidya.appusagetracker.data.preferences.WidgetTheme
import com.abhinavvaidya.appusagetracker.widget.WidgetUpdateWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * UI State for Widget Settings screen.
 */
data class WidgetSettingsUiState(
    val selectedTheme: WidgetTheme = WidgetTheme.DARK_MINIMAL,
    val dailyGoalHours: Float = 4f,
    val showTopApp: Boolean = true,
    val isLoading: Boolean = true
)

/**
 * ViewModel for Widget Settings screen.
 *
 * Manages widget preferences including:
 * - Theme selection
 * - Daily usage goal
 * - Show/hide top app toggle
 *
 * Triggers widget update when settings change.
 */
class WidgetSettingsViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "WidgetSettingsVM"
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
                // Combine all preference flows into a single state
                combine(
                    preferencesRepository.widgetThemeFlow,
                    preferencesRepository.dailyUsageGoalFlow,
                    preferencesRepository.showTopAppFlow
                ) { theme, goalMs, showTopApp ->
                    WidgetSettingsUiState(
                        selectedTheme = theme,
                        dailyGoalHours = goalMs / (60 * 60 * 1000f), // Convert ms to hours
                        showTopApp = showTopApp,
                        isLoading = false
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

    /**
     * Updates the widget theme preference.
     */
    fun setTheme(theme: WidgetTheme) {
        viewModelScope.launch {
            Log.d(TAG, "Setting theme: ${theme.displayName}")
            preferencesRepository.setWidgetTheme(theme)
            triggerWidgetUpdate()
        }
    }

    /**
     * Updates the daily usage goal.
     * @param hours Goal in hours (1-8)
     */
    fun setDailyGoal(hours: Float) {
        viewModelScope.launch {
            val goalMs = (hours * 60 * 60 * 1000).toLong()
            Log.d(TAG, "Setting daily goal: ${hours}h ($goalMs ms)")
            preferencesRepository.setDailyUsageGoal(goalMs)
            // Don't trigger widget update for slider changes (too frequent)
        }
    }

    /**
     * Toggles showing the top app in the widget.
     */
    fun setShowTopApp(show: Boolean) {
        viewModelScope.launch {
            Log.d(TAG, "Setting show top app: $show")
            preferencesRepository.setShowTopApp(show)
            triggerWidgetUpdate()
        }
    }

    /**
     * Triggers an immediate widget update after settings change.
     */
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

