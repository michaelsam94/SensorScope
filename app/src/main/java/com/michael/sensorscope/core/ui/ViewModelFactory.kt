package com.michael.sensorscope.core.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.michael.sensorscope.SensorScopeApp
import com.michael.sensorscope.feature.dashboard.DashboardViewModel
import com.michael.sensorscope.feature.sensor_detail.SensorDetailViewModel
import com.michael.sensorscope.feature.thermal_stress.ThermalStressViewModel
import com.michael.sensorscope.feature.battery_telemetry.BatteryTelemetryViewModel
import com.michael.sensorscope.feature.calibration.CalibrationViewModel
import com.michael.sensorscope.feature.report_preview.ReportPreviewViewModel

@Suppress("UNCHECKED_CAST")
class ViewModelFactory(private val app: SensorScopeApp) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val container = app.container
        return when {
            modelClass.isAssignableFrom(DashboardViewModel::class.java) -> {
                DashboardViewModel(container.observeAllSensorHealthUseCase, container.sensorRepository) as T
            }
            modelClass.isAssignableFrom(SensorDetailViewModel::class.java) -> {
                SensorDetailViewModel(container.observeSensorStreamUseCase, container.sensorRepository) as T
            }
            modelClass.isAssignableFrom(ThermalStressViewModel::class.java) -> {
                ThermalStressViewModel(app, container.thermalRepository) as T
            }
            modelClass.isAssignableFrom(BatteryTelemetryViewModel::class.java) -> {
                BatteryTelemetryViewModel(container.observeBatteryTelemetryUseCase) as T
            }
            modelClass.isAssignableFrom(CalibrationViewModel::class.java) -> {
                CalibrationViewModel(container.sensorRepository, container.captureCalibrationBaselineUseCase) as T
            }
            modelClass.isAssignableFrom(ReportPreviewViewModel::class.java) -> {
                ReportPreviewViewModel(container.sensorRepository, container.batteryRepository, container.thermalRepository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}
