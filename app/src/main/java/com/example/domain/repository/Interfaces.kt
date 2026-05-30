package com.example.domain.repository

import com.example.domain.model.BatteryReading
import com.example.domain.model.SensorBaseline
import com.example.domain.model.SensorInfo
import com.example.domain.model.SensorReading
import com.example.domain.model.SensorStatus
import com.example.domain.model.ThermalReading
import com.example.domain.model.ThermalStressLog
import kotlinx.coroutines.flow.Flow

interface SensorRepository {
    fun getAvailableSensors(): List<SensorInfo>
    fun observeAllSensorHealth(): Flow<Map<Int, SensorStatus>>
    fun observeSensorStream(sensorType: Int): Flow<SensorReading>
    
    // Calibration baseline operations
    fun observeBaselines(): Flow<List<SensorBaseline>>
    suspend fun getBaselineForSensor(sensorType: Int): SensorBaseline?
    suspend fun saveBaseline(baseline: SensorBaseline)
    suspend fun clearBaseline(sensorType: Int)
    suspend fun clearAllBaselines()
    suspend fun captureBaseline(sensorType: Int): SensorBaseline
}

interface BatteryRepository {
    fun observeBatteryTelemetry(): Flow<BatteryReading>
}

interface ThermalRepository {
    fun observeThermalReadings(): Flow<ThermalReading>
    fun observeStressLogs(): Flow<List<ThermalStressLog>>
    suspend fun insertStressLog(log: ThermalStressLog)
    suspend fun clearStressLogs()
}
