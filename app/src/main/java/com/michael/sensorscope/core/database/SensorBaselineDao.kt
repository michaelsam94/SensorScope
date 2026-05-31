package com.michael.sensorscope.core.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SensorBaselineDao {
    @Query("SELECT * FROM sensor_baselines")
    fun getAllBaselinesFlow(): Flow<List<SensorBaselineEntity>>

    @Query("SELECT * FROM sensor_baselines WHERE sensorType = :sensorType")
    suspend fun getBaselineForSensor(sensorType: Int): SensorBaselineEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBaseline(baseline: SensorBaselineEntity)

    @Query("DELETE FROM sensor_baselines WHERE sensorType = :sensorType")
    suspend fun deleteBaseline(sensorType: Int)

    @Query("DELETE FROM sensor_baselines")
    suspend fun clearAllBaselines()
}
