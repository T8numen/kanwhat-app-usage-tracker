package com.abhinavvaidya.appusagetracker.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_usage")
data class AppUsageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val packageName: String,
    val appName: String,
    val usageTimeMillis: Long,
    val date: String
)

