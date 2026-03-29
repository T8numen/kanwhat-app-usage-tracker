package com.abhinavvaidya.appusagetracker.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.abhinavvaidya.appusagetracker.data.local.UsageRepository
import com.abhinavvaidya.appusagetracker.data.preferences.WidgetPreferencesRepository
import com.abhinavvaidya.appusagetracker.domain.model.AppUsageInfo
import com.abhinavvaidya.appusagetracker.util.DurationFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

data class WeeklyDayUsage(
    val date: String,
    val displayDate: String,
    val totalTimeMillis: Long,
    val apps: List<AppUsageInfo>
)

data class WeeklyUiState(
    val isLoading: Boolean = false,
    val dayUsages: List<WeeklyDayUsage> = emptyList(),
    val topApps: List<AppUsageInfo> = emptyList(),
    val totalWeeklyTime: String = "",
    val averageDailyTimeMillis: Long = 0L,
    val rangeStartDate: String = "",
    val rangeEndDate: String = "",
    val showPackageName: Boolean = true,
    val backgroundImageUri: String? = null,
    val error: String? = null
)

class WeeklyViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "WeeklyViewModel"
    }

    private val repository = UsageRepository(application)
    private val preferencesRepository = WidgetPreferencesRepository(application)

    private val _uiState = MutableStateFlow(
        WeeklyUiState(totalWeeklyTime = DurationFormatter.formatCompact(application, 0L))
    )
    val uiState: StateFlow<WeeklyUiState> = _uiState.asStateFlow()

    init {
        loadWeeklyUsage()
    }

    fun loadWeeklyUsage() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                if (!repository.hasUsagePermission()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Usage access permission required"
                    )
                    return@launch
                }

                val weeklyData = withContext(Dispatchers.IO) {
                    val displayFormat = SimpleDateFormat("MM/dd", Locale.getDefault())

                    val dayUsages = (6 downTo 0).map { daysAgo ->
                        val date = repository.getDateString(daysAgo)
                        val apps = repository.getUsageForDateDirect(daysAgo)
                        val total = apps.sumOf { it.usageTimeMillis }

                        val displayDate = try {
                            val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date)
                            if (parsed != null) displayFormat.format(parsed) else date
                        } catch (_: Exception) {
                            date
                        }

                        WeeklyDayUsage(
                            date = date,
                            displayDate = displayDate,
                            totalTimeMillis = total,
                            apps = apps
                        )
                    }

                    val appAggregate = linkedMapOf<String, AppUsageInfo>()
                    dayUsages.flatMap { it.apps }.forEach { app ->
                        val existing = appAggregate[app.packageName]
                        if (existing == null) {
                            appAggregate[app.packageName] = app
                        } else {
                            appAggregate[app.packageName] = existing.copy(
                                usageTimeMillis = existing.usageTimeMillis + app.usageTimeMillis,
                                launchCount = existing.launchCount + app.launchCount,
                                appIcon = existing.appIcon ?: app.appIcon
                            )
                        }
                    }

                    val sortedApps = appAggregate.values.sortedByDescending { it.usageTimeMillis }
                    val totalAppMillis = sortedApps.sumOf { it.usageTimeMillis }.coerceAtLeast(1L)
                    val topApps = sortedApps.take(30).mapIndexed { index, app ->
                        app.copy(
                            usagePercentage = (app.usageTimeMillis.toFloat() / totalAppMillis.toFloat()) * 100f,
                            rank = index + 1
                        )
                    }

                    val totalWeeklyMillis = dayUsages.sumOf { it.totalTimeMillis }
                    val averageDailyMillis = if (dayUsages.isEmpty()) 0L else totalWeeklyMillis / dayUsages.size

                    val backgroundUri = preferencesRepository.getBackgroundImageUri()
                    val showPackageName = preferencesRepository.getShowPackageName()

                    Triple(
                        WeeklyUiState(
                            isLoading = false,
                            dayUsages = dayUsages,
                            topApps = topApps,
                            totalWeeklyTime = DurationFormatter.formatCompact(getApplication(), totalWeeklyMillis),
                            averageDailyTimeMillis = averageDailyMillis,
                            rangeStartDate = dayUsages.firstOrNull()?.date.orEmpty(),
                            rangeEndDate = dayUsages.lastOrNull()?.date.orEmpty(),
                            showPackageName = showPackageName,
                            backgroundImageUri = backgroundUri,
                            error = null
                        ),
                        dayUsages.size,
                        topApps.size
                    )
                }

                _uiState.value = weeklyData.first
                Log.d(TAG, "Loaded weekly data days=${weeklyData.second}, apps=${weeklyData.third}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load weekly data", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load weekly data: ${e.message}"
                )
            }
        }
    }

    fun onResume() {
        loadWeeklyUsage()
    }
}
