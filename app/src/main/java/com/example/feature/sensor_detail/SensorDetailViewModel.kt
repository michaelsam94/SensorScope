package com.example.feature.sensor_detail

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.SensorInfo
import com.example.domain.model.SensorReading
import com.example.domain.repository.SensorRepository
import com.example.domain.usecase.ObserveSensorStreamUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter

sealed class SensorDetailUiState {
    object Loading : SensorDetailUiState()
    data class Success(
        val sensor: SensorInfo,
        val readings: List<SensorReading>,
        val calX: Float,
        val calY: Float,
        val calZ: Float
    ) : SensorDetailUiState()
    object SensorUnavailable : SensorDetailUiState()
}

class SensorDetailViewModel(
    private val observeSensorStreamUseCase: ObserveSensorStreamUseCase,
    private val sensorRepository: SensorRepository
) : ViewModel() {

    private val recentReadings = java.util.ArrayDeque<SensorReading>(300)
    private val _readingsFlow = MutableStateFlow<List<SensorReading>>(emptyList())
    val readingsFlow = _readingsFlow.asStateFlow()

    private val _isCalibrating = MutableStateFlow(false)
    val isCalibrating = _isCalibrating.asStateFlow()

    private val _sensorType = MutableStateFlow<Int>(-1)
    
    val uiState: StateFlow<SensorDetailUiState> = _sensorType.flatMapLatest { type ->
        val sensorInfo = sensorRepository.getAvailableSensors().firstOrNull { it.type == type }
        if (sensorInfo == null) {
            MutableStateFlow(SensorDetailUiState.SensorUnavailable)
        } else {
            combine(
                observeSensorStreamUseCase(type),
                sensorRepository.observeAllSensorHealth()
            ) { reading, healthMap ->
                synchronized(recentReadings) {
                    if (recentReadings.size >= 300) {
                        recentReadings.pollFirst()
                    }
                    recentReadings.addLast(reading)
                    _readingsFlow.value = recentReadings.toList()
                }
                
                val sensorHealth = healthMap[type]
                SensorDetailUiState.Success(
                    sensor = sensorInfo,
                    readings = _readingsFlow.value,
                    calX = sensorHealth?.baselineX ?: 0f,
                    calY = sensorHealth?.baselineY ?: 0f,
                    calZ = sensorHealth?.baselineZ ?: 0f
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SensorDetailUiState.Loading
    )

    fun initialize(sensorType: Int) {
        _sensorType.value = sensorType
    }

    fun calibrateSensor(sensorType: Int) {
        viewModelScope.launch {
            _isCalibrating.value = true
            try {
                sensorRepository.captureBaseline(sensorType)
            } catch (e: Exception) {
                // handle logs safely
            } finally {
                _isCalibrating.value = false
            }
        }
    }

    fun clearCalibration(sensorType: Int) {
        viewModelScope.launch {
            sensorRepository.clearBaseline(sensorType)
        }
    }

    fun exportCsvSnapshot(context: Context, sensorType: Int): File? {
        val sensorInfo = sensorRepository.getAvailableSensors().firstOrNull { it.type == sensorType } ?: return null
        val readingsSnapshot = readingsFlow.value
        if (readingsSnapshot.isEmpty()) return null

        try {
            val exportDir = File(context.getExternalFilesDir(null), "exports")
            if (!exportDir.exists()) exportDir.mkdirs()
            
            val file = File(exportDir, "sensorscope_${sensorInfo.name.replace(" ", "_")}_${System.currentTimeMillis()}.csv")
            val writer = FileWriter(file)
            writer.append("Timestamp,X_or_Val,Y,Z\n")
            readingsSnapshot.forEach { r ->
                writer.append("${r.timestamp},${r.x},${r.y},${r.z}\n")
            }
            writer.flush()
            writer.close()
            return file
        } catch (e: Exception) {
            return null
        }
    }
}
