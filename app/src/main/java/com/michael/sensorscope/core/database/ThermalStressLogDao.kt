package com.michael.sensorscope.core.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ThermalStressLogDao {
    @Query("SELECT * FROM thermal_stress_logs ORDER BY timestamp DESC")
    fun getAllLogsFlow(): Flow<List<ThermalStressLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ThermalStressLogEntity)

    @Query("DELETE FROM thermal_stress_logs")
    suspend fun clearAllLogs()
}
