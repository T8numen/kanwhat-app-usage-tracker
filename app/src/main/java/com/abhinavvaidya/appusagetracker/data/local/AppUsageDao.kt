package com.abhinavvaidya.appusagetracker.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppUsageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsage(usage: AppUsageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(usages: List<AppUsageEntity>)

    @Query("SELECT * FROM app_usage WHERE date = :date ORDER BY usageTimeMillis DESC")
    fun getUsageForDate(date: String): Flow<List<AppUsageEntity>>

    @Query("SELECT * FROM app_usage WHERE date IN (:dates) ORDER BY date DESC, usageTimeMillis DESC")
    fun getUsageForDates(dates: List<String>): Flow<List<AppUsageEntity>>

    @Query("DELETE FROM app_usage WHERE date = :date")
    suspend fun deleteUsageForDate(date: String)
}

