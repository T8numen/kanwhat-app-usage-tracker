package com.abhinavvaidya.appusagetracker.domain.model

data class DailyUsageSummary(
    val date: String,
    val totalTimeMillis: Long,
    val apps: List<AppUsageInfo>
) {
    val formattedTotalTime: String
        get() {
            val hours = totalTimeMillis / (1000 * 60 * 60)
            val minutes = (totalTimeMillis / (1000 * 60)) % 60
            return "${hours}h ${minutes}m"
        }
}

