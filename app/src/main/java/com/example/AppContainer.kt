package com.example

import android.content.Context
import com.example.core.battery.BatteryDataSource
import com.example.core.battery.BatteryDataSourceImpl
import com.example.core.database.SensorScopeDatabase
import com.example.core.sensor.SensorDataSource
import com.example.core.sensor.SensorDataSourceImpl
import com.example.data.repository.BatteryRepositoryImpl
import com.example.data.repository.SensorRepositoryImpl
import com.example.data.repository.ThermalRepositoryImpl
import com.example.domain.repository.BatteryRepository
import com.example.domain.repository.SensorRepository
import com.example.domain.repository.ThermalRepository
import com.example.domain.usecase.CaptureCalibrationBaselineUseCase
import com.example.domain.usecase.ObserveAllSensorHealthUseCase
import com.example.domain.usecase.ObserveBatteryTelemetryUseCase
import com.example.domain.usecase.ObserveSensorStreamUseCase

class AppContainer(private val context: Context) {
    val database: SensorScopeDatabase by lazy {
        SensorScopeDatabase.getDatabase(context)
    }

    val sensorDataSource: SensorDataSource by lazy {
        SensorDataSourceImpl(context)
    }

    val batteryDataSource: BatteryDataSource by lazy {
        BatteryDataSourceImpl(context)
    }

    val sensorRepository: SensorRepository by lazy {
        SensorRepositoryImpl(sensorDataSource, database.sensorBaselineDao())
    }

    val batteryRepository: BatteryRepository by lazy {
        BatteryRepositoryImpl(batteryDataSource)
    }

    val thermalRepository: ThermalRepository by lazy {
        ThermalRepositoryImpl(context, batteryDataSource, database.thermalStressLogDao())
    }

    // Use Cases
    val observeAllSensorHealthUseCase by lazy {
        ObserveAllSensorHealthUseCase(sensorRepository)
    }

    val observeSensorStreamUseCase by lazy {
        ObserveSensorStreamUseCase(sensorRepository)
    }

    val captureCalibrationBaselineUseCase by lazy {
        CaptureCalibrationBaselineUseCase(sensorRepository)
    }

    val observeBatteryTelemetryUseCase by lazy {
        ObserveBatteryTelemetryUseCase(batteryRepository)
    }
}
