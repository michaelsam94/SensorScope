package com.michael.sensorscope.domain.usecase

import com.michael.sensorscope.domain.model.BatteryReading
import com.michael.sensorscope.domain.model.SensorBaseline
import com.michael.sensorscope.domain.model.SensorStatus
import com.michael.sensorscope.domain.model.SensorReading
import com.michael.sensorscope.domain.repository.BatteryRepository
import com.michael.sensorscope.domain.repository.SensorRepository
import kotlinx.coroutines.flow.Flow

class ObserveAllSensorHealthUseCase(private val sensorRepository: SensorRepository) {
    operator fun invoke(): Flow<Map<Int, SensorStatus>> {
        return sensorRepository.observeAllSensorHealth()
    }
}

class ObserveSensorStreamUseCase(private val sensorRepository: SensorRepository) {
    operator fun invoke(sensorType: Int): Flow<SensorReading> {
        return sensorRepository.observeSensorStream(sensorType)
    }
}

class CaptureCalibrationBaselineUseCase(private val sensorRepository: SensorRepository) {
    suspend operator fun invoke(sensorType: Int): SensorBaseline {
        return sensorRepository.captureBaseline(sensorType)
    }
}

class ObserveBatteryTelemetryUseCase(private val batteryRepository: BatteryRepository) {
    operator fun invoke(): Flow<BatteryReading> {
        return batteryRepository.observeBatteryTelemetry()
    }
}
