package com.michael.sensorscope.data.repository

import com.michael.sensorscope.core.database.SensorBaselineDao
import com.michael.sensorscope.core.database.SensorBaselineEntity
import com.michael.sensorscope.core.sensor.SensorDataSource
import com.michael.sensorscope.domain.model.SensorBaseline
import com.michael.sensorscope.domain.model.SensorHealth
import com.michael.sensorscope.domain.model.SensorInfo
import com.michael.sensorscope.domain.model.SensorReading
import com.michael.sensorscope.domain.model.SensorStatus
import com.michael.sensorscope.domain.repository.SensorRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class SensorRepositoryImpl(
    private val sensorDataSource: SensorDataSource,
    private val sensorBaselineDao: SensorBaselineDao
) : SensorRepository {

    override fun getAvailableSensors(): List<SensorInfo> {
        return sensorDataSource.getSystemSensors()
    }

    override fun observeAllSensorHealth(): Flow<Map<Int, SensorStatus>> {
        val available = getAvailableSensors()
        return sensorBaselineDao.getAllBaselinesFlow().map { baselines ->
            val baselineMap = baselines.associateBy { it.sensorType }
            available.associate { info ->
                val baseline = baselineMap[info.type]
                val status = SensorStatus(
                    sensorInfo = info,
                    isAvailable = true,
                    health = SensorHealth.OK,
                    baselineX = baseline?.xOffset ?: 0f,
                    baselineY = baseline?.yOffset ?: 0f,
                    baselineZ = baseline?.zOffset ?: 0f
                )
                info.type to status
            }
        }.flowOn(Dispatchers.IO)
    }

    override fun observeSensorStream(sensorType: Int): Flow<SensorReading> {
        return flow {
            val baseline = sensorBaselineDao.getBaselineForSensor(sensorType)
            val xOff = baseline?.xOffset ?: 0f
            val yOff = baseline?.yOffset ?: 0f
            val zOff = baseline?.zOffset ?: 0f
            
            sensorDataSource.observeSensor(sensorType).collect { reading ->
                val calibratedValues = reading.values.mapIndexed { idx, value ->
                    when (idx) {
                        0 -> value - xOff
                        1 -> value - yOff
                        2 -> value - zOff
                        else -> value
                    }
                }
                emit(SensorReading(reading.timestamp, calibratedValues))
            }
        }.flowOn(Dispatchers.Default)
    }

    override fun observeBaselines(): Flow<List<SensorBaseline>> {
        return sensorBaselineDao.getAllBaselinesFlow().map { list ->
            list.map { SensorBaseline(it.sensorType, it.sensorName, it.xOffset, it.yOffset, it.zOffset, it.timestamp) }
        }
    }

    override suspend fun getBaselineForSensor(sensorType: Int): SensorBaseline? {
        return withContext(Dispatchers.IO) {
            sensorBaselineDao.getBaselineForSensor(sensorType)?.let {
                SensorBaseline(it.sensorType, it.sensorName, it.xOffset, it.yOffset, it.zOffset, it.timestamp)
            }
        }
    }

    override suspend fun saveBaseline(baseline: SensorBaseline) {
        withContext(Dispatchers.IO) {
            sensorBaselineDao.insertBaseline(
                SensorBaselineEntity(
                    sensorType = baseline.sensorType,
                    sensorName = baseline.sensorName,
                    xOffset = baseline.xOffset,
                    yOffset = baseline.yOffset,
                    zOffset = baseline.zOffset,
                    timestamp = baseline.timestamp
                )
            )
        }
    }

    override suspend fun clearBaseline(sensorType: Int) {
        withContext(Dispatchers.IO) {
            sensorBaselineDao.deleteBaseline(sensorType)
        }
    }

    override suspend fun clearAllBaselines() {
        withContext(Dispatchers.IO) {
            sensorBaselineDao.clearAllBaselines()
        }
    }

    override suspend fun captureBaseline(sensorType: Int): SensorBaseline {
        return withContext(Dispatchers.Default) {
            val sensorInfo = getAvailableSensors().firstOrNull { it.type == sensorType }
                ?: throw IllegalArgumentException("Sensor not available")
            
            val samplingFlow = sensorDataSource.observeSensor(sensorType)
            val collected = mutableListOf<SensorReading>()
            
            try {
                kotlinx.coroutines.withTimeout(1500) {
                    samplingFlow.collect { reading ->
                        collected.add(reading)
                        if (collected.size >= 25) {
                            throw kotlinx.coroutines.CancellationException("Completed capture")
                        }
                    }
                }
            } catch (e: Exception) {
               // Safely end capture flow on timeout/cancel
            }
            
            val avgX = if (collected.isNotEmpty()) collected.map { it.x }.average().toFloat() else 0f
            val avgY = if (collected.isNotEmpty()) collected.map { it.y }.average().toFloat() else 0f
            val avgZ = if (collected.isNotEmpty()) collected.map { it.z }.average().toFloat() else 0f
            
            val baseline = SensorBaseline(
                sensorType = sensorType,
                sensorName = sensorInfo.name,
                xOffset = avgX,
                yOffset = avgY,
                zOffset = avgZ,
                timestamp = System.currentTimeMillis()
            )
            
            saveBaseline(baseline)
            baseline
        }
    }
}
