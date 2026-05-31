package com.michael.sensorscope.feature.report_preview

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.michael.sensorscope.domain.model.BatteryReading
import com.michael.sensorscope.domain.model.SensorStatus
import com.michael.sensorscope.domain.model.ThermalStressLog
import com.michael.sensorscope.domain.repository.BatteryRepository
import com.michael.sensorscope.domain.repository.SensorRepository
import com.michael.sensorscope.domain.repository.ThermalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter

sealed class ReportUiState {
    object Loading : ReportUiState()
    data class Success(
        val reportText: String,
        val reportFile: File?
    ) : ReportUiState()
}

class ReportPreviewViewModel(
    private val sensorRepository: SensorRepository,
    private val batteryRepository: BatteryRepository,
    private val thermalRepository: ThermalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ReportUiState>(ReportUiState.Loading)
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    fun generateReport(context: Context) {
        viewModelScope.launch {
            _uiState.value = ReportUiState.Loading
            
            val sensors = sensorRepository.observeAllSensorHealth().first()
            val battery = batteryRepository.observeBatteryTelemetry().first()
            val stressLogs = thermalRepository.observeStressLogs().first()
            
            val sb = java.lang.StringBuilder()
            sb.append("=========================================\n")
            sb.append("     SENSORSCOPE HARDWARE DIAGNOSTICS    \n")
            sb.append("=========================================\n")
            sb.append("Report Timestamp: ${java.util.Date()}\n\n")
            
            sb.append("--- [SYSTEM POWER QUALITY REPORT] ---\n")
            sb.append("Charge Level: ${battery.level}%\n")
            sb.append("Cell Voltage: ${battery.voltage / 1000f} V\n")
            sb.append("Heat Index: ${battery.temperature} °C\n")
            sb.append("Cell Chemistry: ${battery.technology}\n")
            sb.append("Health Status: ${battery.healthString}\n")
            sb.append("System Flags: ${if (battery.isAnomaly) "ANOMALY WARNINGS DETECTED" else "ALL POWER SYSTEMS STABLE"}\n\n")
            
            sb.append("--- [THERMAL THROTTLING STRESS SANDBOX LOG] ---\n")
            val latestStress = stressLogs.firstOrNull()
            if (latestStress != null) {
                sb.append("Last stress log timestamp: ${java.util.Date(latestStress.timestamp)}\n")
                sb.append("Max CPU Core Temp reached: ${latestStress.maxCpuTemp} °C\n")
                sb.append("Max Battery Temp reached: ${latestStress.maxBatteryTemp} °C\n")
                sb.append("Execution duration: ${latestStress.durationMs / 1000} seconds\n")
                sb.append("Safety thermal halt triggered: ${if (latestStress.safetyStopTriggered) "YES (45°C safety system trigger)" else "NO"}\n")
                sb.append("Verdict: ${latestStress.healthAssessment}\n\n")
            } else {
                sb.append("No local CPU Stress Sandboxes executed on this unit yet.\n\n")
            }
            
            sb.append("--- [SENSOR SYSTEM CALIBRATION LOGS] ---\n")
            sensors.values.forEach { status ->
                val hasCal = status.baselineX != 0f || status.baselineY != 0f || status.baselineZ != 0f
                sb.append("• [${status.sensorInfo.category.name}] ${status.sensorInfo.name}\n")
                sb.append("  Vendor: ${status.sensorInfo.vendor} | Max Range: ${status.sensorInfo.maximumRange}\n")
                sb.append("  Diagnostics Health State: ${status.health.name}\n")
                if (hasCal) {
                    sb.append("  Calibration offsets active -> X: ${status.baselineX}, Y: ${status.baselineY}, Z: ${status.baselineZ}\n")
                } else {
                    sb.append("  Calibration offsets active -> NONE (Default raw factory offsets active)\n")
                }
                sb.append("\n")
            }
            
            val reportText = sb.toString()
            
            val reportFile = try {
                val reportsDir = File(context.getExternalFilesDir(null), "exports")
                if (!reportsDir.exists()) reportsDir.mkdirs()
                val file = File(reportsDir, "sensorscope_health_report_${System.currentTimeMillis()}.txt")
                val writer = FileWriter(file)
                writer.write(reportText)
                writer.flush()
                writer.close()
                file
            } catch (e: Exception) {
                null
            }
            
            _uiState.value = ReportUiState.Success(reportText, reportFile)
        }
    }
}
