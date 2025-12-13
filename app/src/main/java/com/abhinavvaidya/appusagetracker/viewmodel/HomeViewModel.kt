package com.abhinavvaidya.appusagetracker.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.abhinavvaidya.appusagetracker.data.local.UsageRepository
import com.abhinavvaidya.appusagetracker.domain.model.AppUsageInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class HomeUiState(
    val isLoading: Boolean = false,
    val hasPermission: Boolean = false,
    val totalScreenTime: String = "0h 0m",
    val totalScreenTimeMillis: Long = 0L,
    val appUsageList: List<AppUsageInfo> = emptyList(),
    val error: String? = null
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "HomeViewModel"
    }

    private val repository = UsageRepository(application)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        Log.d(TAG, "HomeViewModel initialized")
        checkPermissionAndLoad()
    }

    /**
     * Checks if permission is granted and loads data if available.
     * This should be called on init and when returning from settings.
     */
    fun checkPermissionAndLoad() {
        val hasPermission = repository.hasUsagePermission()
        Log.d(TAG, "Permission check: $hasPermission")

        _uiState.value = _uiState.value.copy(hasPermission = hasPermission)

        if (hasPermission) {
            loadTodayUsage()
        } else {
            // Clear data if permission was revoked
            _uiState.value = _uiState.value.copy(
                appUsageList = emptyList(),
                totalScreenTime = "0h 0m",
                totalScreenTimeMillis = 0L,
                error = null
            )
        }
    }

    /**
     * Loads today's usage data.
     * Called automatically when permission is granted and on app resume.
     * Also updates the widget cache for consistency.
     */
    fun loadTodayUsage() {
        viewModelScope.launch {
            Log.d(TAG, "Loading today's usage...")
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                // Fetch usage on IO thread
                val usageList = withContext(Dispatchers.IO) {
                    repository.refreshTodayUsage()
                    // Also refresh widget cache so widget stays in sync
                    repository.refreshWidgetCache()
                    repository.getTodayUsageDirect()
                }

                Log.d(TAG, "Loaded ${usageList.size} apps with usage data")

                // Calculate total screen time from the usage list
                // This ensures consistency: total = sum of all app times
                val totalMillis = usageList.sumOf { it.usageTimeMillis }
                val totalScreenTime = formatScreenTime(totalMillis)

                Log.d(TAG, "Total screen time: $totalScreenTime ($totalMillis ms)")

                // Log top apps for debugging
                usageList.take(5).forEach { app ->
                    Log.d(TAG, "  ${app.appName}: ${app.usageTimeMillis / 1000 / 60}m")
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    appUsageList = usageList,
                    totalScreenTime = totalScreenTime,
                    totalScreenTimeMillis = totalMillis,
                    error = null
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load usage data", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load usage data: ${e.message}"
                )
            }
        }
    }

    /**
     * Formats screen time in milliseconds to a human-readable string.
     */
    private fun formatScreenTime(totalMillis: Long): String {
        val hours = totalMillis / (1000 * 60 * 60)
        val minutes = (totalMillis / (1000 * 60)) % 60

        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "0m"
        }
    }

    /**
     * Called when app resumes to automatically refresh data.
     * This handles cases like:
     * - Returning from settings after granting permission
     * - Coming back to the app after using other apps
     */
    fun onResume() {
        Log.d(TAG, "onResume called")
        checkPermissionAndLoad()
    }
}

