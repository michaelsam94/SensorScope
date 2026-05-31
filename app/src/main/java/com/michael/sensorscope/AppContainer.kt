package com.michael.sensorscope

import android.content.Context
import com.michael.sensorscope.core.battery.BatteryDataSource
import com.michael.sensorscope.core.battery.BatteryDataSourceImpl
import com.michael.sensorscope.core.database.SensorScopeDatabase
import com.michael.sensorscope.core.sensor.SensorDataSource
import com.michael.sensorscope.core.sensor.SensorDataSourceImpl
import com.michael.sensorscope.data.repository.BatteryRepositoryImpl
import com.michael.sensorscope.data.repository.SensorRepositoryImpl
import com.michael.sensorscope.data.repository.ThermalRepositoryImpl
import com.michael.sensorscope.domain.repository.BatteryRepository
import com.michael.sensorscope.domain.repository.SensorRepository
import com.michael.sensorscope.domain.repository.ThermalRepository
import com.michael.sensorscope.domain.usecase.CaptureCalibrationBaselineUseCase
import com.michael.sensorscope.domain.usecase.ObserveAllSensorHealthUseCase
import com.michael.sensorscope.domain.usecase.ObserveBatteryTelemetryUseCase
import com.michael.sensorscope.domain.usecase.ObserveSensorStreamUseCase

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
