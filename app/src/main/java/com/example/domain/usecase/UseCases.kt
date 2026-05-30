package com.example.domain.usecase

import com.example.domain.model.BatteryReading
import com.example.domain.model.SensorBaseline
import com.example.domain.model.SensorStatus
import com.example.domain.model.SensorReading
import com.example.domain.repository.BatteryRepository
import com.example.domain.repository.SensorRepository
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
