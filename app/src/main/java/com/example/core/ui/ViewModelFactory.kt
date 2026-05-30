package com.example.core.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.SensorScopeApp
import com.example.feature.dashboard.DashboardViewModel
import com.example.feature.sensor_detail.SensorDetailViewModel
import com.example.feature.thermal_stress.ThermalStressViewModel
import com.example.feature.battery_telemetry.BatteryTelemetryViewModel
import com.example.feature.calibration.CalibrationViewModel
import com.example.feature.report_preview.ReportPreviewViewModel

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
