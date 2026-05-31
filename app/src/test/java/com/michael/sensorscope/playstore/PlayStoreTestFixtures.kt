package com.michael.sensorscope.playstore

import com.michael.sensorscope.domain.model.BatteryReading
import com.michael.sensorscope.domain.model.SensorBaseline
import com.michael.sensorscope.domain.model.SensorCategory
import com.michael.sensorscope.domain.model.SensorHealth
import com.michael.sensorscope.domain.model.SensorInfo
import com.michael.sensorscope.domain.model.SensorReading
import com.michael.sensorscope.domain.model.SensorStatus
import com.michael.sensorscope.domain.model.ThermalReading
import com.michael.sensorscope.domain.model.ThermalStressLog
import com.michael.sensorscope.domain.repository.BatteryRepository
import com.michael.sensorscope.domain.repository.SensorRepository
import com.michael.sensorscope.domain.repository.ThermalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

object PlayStoreTestFixtures {
    val sensors = listOf(
        SensorInfo(1, "Accelerometer", "SensorScope Labs", 3, 78.4f, 0.001f, 0.12f, 5000, SensorCategory.MOTION),
        SensorInfo(2, "Gyroscope", "SensorScope Labs", 2, 34.9f, 0.002f, 0.24f, 5000, SensorCategory.MOTION),
        SensorInfo(3, "Ambient Light", "Android Reference", 1, 10000f, 1f, 0.05f, 200000, SensorCategory.ENVIRONMENT),
        SensorInfo(4, "Barometer", "Android Reference", 1, 1100f, 0.01f, 0.08f, 100000, SensorCategory.ENVIRONMENT),
        SensorInfo(5, "Magnetometer", "SensorScope Labs", 4, 2000f, 0.15f, 0.18f, 10000, SensorCategory.POSITION),
        SensorInfo(6, "Proximity", "Android Reference", 1, 8f, 1f, 0.04f, 100000, SensorCategory.UNKNOWN),
    )

    val statusMap = sensors.associate { sensor ->
        sensor.type to SensorStatus(
            sensorInfo = sensor,
            health = when (sensor.type) {
                3 -> SensorHealth.DEGRADED
                6 -> SensorHealth.FAULT
                else -> SensorHealth.OK
            },
            baselineX = if (sensor.type == 1) 0.014f else 0f,
            baselineY = if (sensor.type == 1) -0.008f else 0f,
            baselineZ = if (sensor.type == 1) 0.122f else 0f,
        )
    }

    val battery = BatteryReading(
        level = 87,
        health = 2,
        voltage = 4128,
        currentMicroAmp = -428000,
        temperature = 32.6f,
        technology = "Li-ion",
        isCharging = false,
    )

    val stressLogs = listOf(
        ThermalStressLog(
            id = 1,
            timestamp = 1_778_688_000_000,
            maxCpuTemp = 42.8f,
            maxBatteryTemp = 36.4f,
            durationMs = 30_000,
            safetyStopTriggered = false,
            throttlingDetected = false,
            healthAssessment = "Thermal envelope remained stable during sandbox load.",
        ),
        ThermalStressLog(
            id = 2,
            timestamp = 1_778_601_600_000,
            maxCpuTemp = 44.1f,
            maxBatteryTemp = 37.2f,
            durationMs = 30_000,
            safetyStopTriggered = false,
            throttlingDetected = true,
            healthAssessment = "Minor throttling detected near the end of the run.",
        ),
    )

    fun readings(sensorType: Int): List<SensorReading> {
        return (0 until 56).map { index ->
            val t = index / 6f
            SensorReading(
                timestamp = 1_778_688_000_000 + index * 16,
                values = when (sensorType) {
                    3 -> listOf(420f + index * 8f)
                    else -> listOf(
                        kotlin.math.sin(t) * 4.2f,
                        kotlin.math.cos(t * 0.8f) * 3.4f,
                        9.8f + kotlin.math.sin(t * 0.45f) * 0.5f,
                    )
                },
            )
        }
    }
}

class FakeSensorRepository : SensorRepository {
    private val healthFlow = MutableStateFlow(PlayStoreTestFixtures.statusMap)
    private val baselines = mutableMapOf<Int, SensorBaseline>()

    override fun getAvailableSensors(): List<SensorInfo> = PlayStoreTestFixtures.sensors
    override fun observeAllSensorHealth(): Flow<Map<Int, SensorStatus>> = healthFlow
    override fun observeSensorStream(sensorType: Int): Flow<SensorReading> =
        flowOf(*PlayStoreTestFixtures.readings(sensorType).toTypedArray())

    override fun observeBaselines(): Flow<List<SensorBaseline>> = flowOf(baselines.values.toList())
    override suspend fun getBaselineForSensor(sensorType: Int): SensorBaseline? = baselines[sensorType]
    override suspend fun saveBaseline(baseline: SensorBaseline) {
        baselines[baseline.sensorType] = baseline
    }

    override suspend fun clearBaseline(sensorType: Int) {
        baselines.remove(sensorType)
    }

    override suspend fun clearAllBaselines() {
        baselines.clear()
    }

    override suspend fun captureBaseline(sensorType: Int): SensorBaseline {
        val sensor = getAvailableSensors().first { it.type == sensorType }
        return SensorBaseline(sensorType, sensor.name, 0.014f, -0.008f, 0.122f, 1_778_688_000_000)
            .also { saveBaseline(it) }
    }
}

class FakeBatteryRepository : BatteryRepository {
    override fun observeBatteryTelemetry(): Flow<BatteryReading> = flowOf(PlayStoreTestFixtures.battery)
}

class FakeThermalRepository : ThermalRepository {
    override fun observeThermalReadings(): Flow<ThermalReading> = flowOf(
        ThermalReading(1_778_688_000_000, 34.1f, 39.2f),
    )

    override fun observeStressLogs(): Flow<List<ThermalStressLog>> = flowOf(PlayStoreTestFixtures.stressLogs)
    override suspend fun insertStressLog(log: ThermalStressLog) = Unit
    override suspend fun clearStressLogs() = Unit
}
