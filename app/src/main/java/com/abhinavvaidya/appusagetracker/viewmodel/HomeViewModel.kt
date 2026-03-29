package com.abhinavvaidya.appusagetracker.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.abhinavvaidya.appusagetracker.data.local.UsageRepository
import com.abhinavvaidya.appusagetracker.data.preferences.AppListMetric
import com.abhinavvaidya.appusagetracker.data.preferences.WidgetPreferencesRepository
import com.abhinavvaidya.appusagetracker.domain.model.AppUsageInfo
import com.abhinavvaidya.appusagetracker.util.DurationFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class HomeUiState(
    val isLoading: Boolean = false,
    val hasPermission: Boolean = false,
    val totalScreenTime: String = "",
    val totalScreenTimeMillis: Long = 0L,
    val appUsageList: List<AppUsageInfo> = emptyList(),
    val selectedDaysAgo: Int = 0,
    val selectedDateLabel: String = "",
    val canGoToNewerDate: Boolean = false,
    val canGoToOlderDate: Boolean = true,
    val listMetric: AppListMetric = AppListMetric.USAGE_TIME,
    val showPackageName: Boolean = true,
    val timeFragmentPercent: Int = 0,
    val launchCount: Int = 0,
    val unlockCount: Int = 0,
    val notificationCount: Int = 0,
    val backgroundImageUri: String? = null,
    val error: String? = null
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "HomeViewModel"
        private const val MAX_DAYS_HISTORY = 30
    }

    private val repository = UsageRepository(application)
    private val preferencesRepository = WidgetPreferencesRepository(application)

    private val _uiState = MutableStateFlow(
        HomeUiState(
            totalScreenTime = DurationFormatter.formatCompact(application, 0L),
            selectedDateLabel = repository.getDateString(0)
        )
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observeListMetric()
        observeShowPackageName()
        observeBackgroundImage()
        checkPermissionAndLoad()
    }

    private fun observeListMetric() {
        viewModelScope.launch {
            preferencesRepository.appListMetricFlow.collect { metric ->
                _uiState.update { it.copy(listMetric = metric) }
            }
        }
    }

    private fun observeBackgroundImage() {
        viewModelScope.launch {
            preferencesRepository.backgroundImageUriFlow.collect { uri ->
                _uiState.update { it.copy(backgroundImageUri = uri) }
            }
        }
    }

    private fun observeShowPackageName() {
        viewModelScope.launch {
            preferencesRepository.showPackageNameFlow.collect { showPackageName ->
                _uiState.update { it.copy(showPackageName = showPackageName) }
            }
        }
    }

    fun checkPermissionAndLoad() {
        val hasPermission = repository.hasUsagePermission()

        _uiState.update { current ->
            current.copy(hasPermission = hasPermission)
        }

        if (hasPermission) {
            loadUsageForSelectedDate()
        } else {
            _uiState.update {
                it.copy(
                    selectedDaysAgo = 0,
                    selectedDateLabel = repository.getDateString(0),
                    canGoToNewerDate = false,
                    canGoToOlderDate = true,
                    appUsageList = emptyList(),
                    totalScreenTime = DurationFormatter.formatCompact(getApplication(), 0L),
                    totalScreenTimeMillis = 0L,
                    timeFragmentPercent = 0,
                    launchCount = 0,
                    unlockCount = 0,
                    notificationCount = 0,
                    error = null
                )
            }
        }
    }

    fun showPreviousDate() {
        val currentDaysAgo = _uiState.value.selectedDaysAgo
        if (currentDaysAgo >= MAX_DAYS_HISTORY) return

        _uiState.update {
            val nextDaysAgo = (it.selectedDaysAgo + 1).coerceAtMost(MAX_DAYS_HISTORY)
            it.copy(
                selectedDaysAgo = nextDaysAgo,
                selectedDateLabel = repository.getDateString(nextDaysAgo),
                canGoToNewerDate = nextDaysAgo > 0,
                canGoToOlderDate = nextDaysAgo < MAX_DAYS_HISTORY
            )
        }
        loadUsageForSelectedDate()
    }

    fun showNextDate() {
        val currentDaysAgo = _uiState.value.selectedDaysAgo
        if (currentDaysAgo <= 0) return

        _uiState.update {
            val nextDaysAgo = (it.selectedDaysAgo - 1).coerceAtLeast(0)
            it.copy(
                selectedDaysAgo = nextDaysAgo,
                selectedDateLabel = repository.getDateString(nextDaysAgo),
                canGoToNewerDate = nextDaysAgo > 0,
                canGoToOlderDate = nextDaysAgo < MAX_DAYS_HISTORY
            )
        }
        loadUsageForSelectedDate()
    }

    fun loadUsageForSelectedDate() {
        viewModelScope.launch {
            val selectedDaysAgo = _uiState.value.selectedDaysAgo

            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val snapshot = withContext(Dispatchers.IO) {
                    repository.getUsageSnapshotForDate(selectedDaysAgo)
                }

                val totalMillis = snapshot.usageList.sumOf { it.usageTimeMillis }
                val totalScreenTime = DurationFormatter.formatCompact(getApplication(), totalMillis)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        appUsageList = snapshot.usageList,
                        totalScreenTime = totalScreenTime,
                        totalScreenTimeMillis = totalMillis,
                        selectedDateLabel = repository.getDateString(selectedDaysAgo),
                        canGoToNewerDate = selectedDaysAgo > 0,
                        canGoToOlderDate = selectedDaysAgo < MAX_DAYS_HISTORY,
                        timeFragmentPercent = snapshot.timeFragmentPercent,
                        launchCount = snapshot.launchCount,
                        unlockCount = snapshot.unlockCount,
                        notificationCount = snapshot.notificationCount,
                        error = null
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load usage data", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load usage data: ${e.message}"
                    )
                }
            }
        }
    }

    fun onResume() {
        checkPermissionAndLoad()
    }
}
