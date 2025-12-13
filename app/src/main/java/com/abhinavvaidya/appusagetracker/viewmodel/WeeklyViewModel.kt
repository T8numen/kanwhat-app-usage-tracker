package com.abhinavvaidya.appusagetracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.abhinavvaidya.appusagetracker.data.local.UsageRepository
import com.abhinavvaidya.appusagetracker.domain.model.AppUsageInfo
import com.abhinavvaidya.appusagetracker.domain.model.DailyUsageSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class WeeklyUiState(
    val isLoading: Boolean = false,
    val dailySummaries: List<DailyUsageSummary> = emptyList(),
    val topApps: List<AppUsageInfo> = emptyList(),
    val totalWeeklyTime: String = "0h 0m",
    val error: String? = null
)

class WeeklyViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = UsageRepository(application)

    private val _uiState = MutableStateFlow(WeeklyUiState())
    val uiState: StateFlow<WeeklyUiState> = _uiState.asStateFlow()

    init {
        loadWeeklyUsage()
    }

    fun loadWeeklyUsage() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                repository.refreshWeeklyUsage()

                repository.getWeeklyUsageFlow().collect { summaries ->
                    val totalMillis = summaries.sumOf { it.totalTimeMillis }
                    val hours = totalMillis / (1000 * 60 * 60)
                    val minutes = (totalMillis / (1000 * 60)) % 60

                    // Aggregate top apps across all days
                    val appUsageMap = mutableMapOf<String, AppUsageInfo>()
                    summaries.flatMap { it.apps }.forEach { app ->
                        val existing = appUsageMap[app.packageName]
                        if (existing != null) {
                            appUsageMap[app.packageName] = existing.copy(
                                usageTimeMillis = existing.usageTimeMillis + app.usageTimeMillis
                            )
                        } else {
                            appUsageMap[app.packageName] = app
                        }
                    }

                    val topApps = appUsageMap.values
                        .sortedByDescending { it.usageTimeMillis }
                        .take(5)

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        dailySummaries = summaries,
                        topApps = topApps.toList(),
                        totalWeeklyTime = "${hours}h ${minutes}m"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load weekly data"
                )
            }
        }
    }

    /**
     * Called when app resumes to automatically refresh data
     */
    fun onResume() {
        loadWeeklyUsage()
    }
}

