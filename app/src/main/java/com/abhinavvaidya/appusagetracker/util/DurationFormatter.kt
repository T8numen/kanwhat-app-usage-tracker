package com.abhinavvaidya.appusagetracker.util

import android.content.Context
import com.abhinavvaidya.appusagetracker.R

object DurationFormatter {

    fun formatCompact(context: Context, millis: Long): String {
        val hours = millis / (1000 * 60 * 60)
        val minutes = (millis / (1000 * 60)) % 60

        return when {
            hours > 0 -> context.getString(R.string.duration_hours_minutes, hours, minutes)
            minutes > 0 -> context.getString(R.string.duration_minutes, minutes)
            else -> context.getString(R.string.duration_zero)
        }
    }

    fun formatDetailed(context: Context, millis: Long): String {
        val hours = millis / (1000 * 60 * 60)
        val minutes = (millis / (1000 * 60)) % 60
        val seconds = (millis / 1000) % 60

        return when {
            hours > 0 -> context.getString(R.string.duration_hours_minutes, hours, minutes)
            minutes > 0 -> context.getString(R.string.duration_minutes_seconds, minutes, seconds)
            else -> context.getString(R.string.duration_seconds, seconds)
        }
    }
}
