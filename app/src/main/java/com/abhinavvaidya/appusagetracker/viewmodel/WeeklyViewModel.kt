package com.abhinavvaidya.appusagetracker.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.abhinavvaidya.appusagetracker.data.local.UsageRepository
import com.abhinavvaidya.appusagetracker.domain.model.AppUsageInfo
import com.abhinavvaidya.appusagetracker.domain.model.DailyUsageSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class WeeklyUiState(
    val isLoading: Boolean = false,
    val dailySummaries: List<DailyUsageSummary> = emptyList(),
    val topApps: List<AppUsageInfo> = emptyList(),
    val totalWeeklyTime: String = "0h 0m",
    val error: String? = null
)

class WeeklyViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "WeeklyViewModel"
    }

    private val repository = UsageRepository(application)

    private val _uiState = MutableStateFlow(WeeklyUiState())
    val uiState: StateFlow<WeeklyUiState> = _uiState.asStateFlow()

    init {
        Log.d(TAG, "WeeklyViewModel initialized")
        loadWeeklyUsage()
    }

    fun loadWeeklyUsage() {
        viewModelScope.launch {
            Log.d(TAG, "Loading weekly usage...")
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                // Check permission first
                if (!repository.hasUsagePermission()) {
                    Log.w(TAG, "No usage permission")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Usage access permission required"
                    )
                    return@launch
                }

                withContext(Dispatchers.IO) {
                    repository.refreshWeeklyUsage()
                }

                repository.getWeeklyUsageFlow().collect { summaries ->
                    Log.d(TAG, "Received ${summaries.size} daily summaries")

                    // Log icons for each summary
                    summaries.forEach { summary ->
                        Log.d(TAG, "Date ${summary.date}: ${summary.apps.size} apps")
                        summary.apps.forEach { app ->
                            Log.v(TAG, "  ${app.appName}: icon=${app.appIcon != null}")
                        }
                    }

                    val totalMillis = summaries.sumOf { it.totalTimeMillis }
                    val hours = totalMillis / (1000 * 60 * 60)
                    val minutes = (totalMillis / (1000 * 60)) % 60

                    Log.d(TAG, "Total weekly time: ${hours}h ${minutes}m")

                    // Aggregate top apps across all days
                    val appUsageMap = mutableMapOf<String, AppUsageInfo>()
                    summaries.flatMap { it.apps }.forEach { app ->
                        val existing = appUsageMap[app.packageName]
                        if (existing != null) {
                            // Preserve appIcon and other fields when aggregating
                            appUsageMap[app.packageName] = existing.copy(
                                usageTimeMillis = existing.usageTimeMillis + app.usageTimeMillis,
                                appIcon = existing.appIcon ?: app.appIcon
                            )
                        } else {
                            appUsageMap[app.packageName] = app
                        }
                    }

                    val topApps = appUsageMap.values
                        .sortedByDescending { it.usageTimeMillis }
                        .take(5)

                    Log.d(TAG, "Top ${topApps.size} apps:")
                    topApps.forEach { app ->
                        Log.d(TAG, "  ${app.appName}: ${app.usageTimeMillis / 1000 / 60}m, icon=${app.appIcon != null}")
                    }

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        dailySummaries = summaries,
                        topApps = topApps.toList(),
                        totalWeeklyTime = "${hours}h ${minutes}m"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load weekly data", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load weekly data: ${e.message}"
                )
            }
        }
    }

    /**
     * Called when app resumes to automatically refresh data
     */
    fun onResume() {
        Log.d(TAG, "onResume called")
        loadWeeklyUsage()
    }
}

