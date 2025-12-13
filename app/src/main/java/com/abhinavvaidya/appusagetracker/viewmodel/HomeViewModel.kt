package com.abhinavvaidya.appusagetracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.abhinavvaidya.appusagetracker.data.local.UsageRepository
import com.abhinavvaidya.appusagetracker.domain.model.AppUsageInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = false,
    val hasPermission: Boolean = false,
    val totalScreenTime: String = "0h 0m",
    val appUsageList: List<AppUsageInfo> = emptyList(),
    val error: String? = null
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = UsageRepository(application)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        checkPermissionAndLoad()
    }

    fun checkPermissionAndLoad() {
        val hasPermission = repository.hasUsagePermission()
        _uiState.value = _uiState.value.copy(hasPermission = hasPermission)

        if (hasPermission) {
            loadTodayUsage()
        }
    }

    fun loadTodayUsage() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                repository.refreshTodayUsage()

                val usageList = repository.getTodayUsageDirect()
                val totalMillis = usageList.sumOf { it.usageTimeMillis }
                val hours = totalMillis / (1000 * 60 * 60)
                val minutes = (totalMillis / (1000 * 60)) % 60

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    appUsageList = usageList,
                    totalScreenTime = "${hours}h ${minutes}m"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load usage data"
                )
            }
        }
    }

    /**
     * Called when app resumes to automatically refresh data
     */
    fun onResume() {
        checkPermissionAndLoad()
    }
}

