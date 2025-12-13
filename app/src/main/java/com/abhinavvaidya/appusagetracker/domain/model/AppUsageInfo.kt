package com.abhinavvaidya.appusagetracker.domain.model

import android.graphics.drawable.Drawable

data class AppUsageInfo(
    val packageName: String,
    val appName: String,
    val usageTimeMillis: Long,
    val appIcon: Drawable? = null,
    val usagePercentage: Float = 0f,
    val rank: Int = 0
) {
    val formattedTime: String
        get() {
            val hours = usageTimeMillis / (1000 * 60 * 60)
            val minutes = (usageTimeMillis / (1000 * 60)) % 60
            val seconds = (usageTimeMillis / 1000) % 60
            return when {
                hours > 0 -> "${hours}h ${minutes}m"
                minutes > 0 -> "${minutes}m ${seconds}s"
                else -> "${seconds}s"
            }
        }

    val formattedPercentage: String
        get() = String.format("%.1f%%", usagePercentage)
}

