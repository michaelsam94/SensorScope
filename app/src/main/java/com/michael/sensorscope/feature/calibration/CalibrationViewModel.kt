package com.michael.sensorscope.feature.calibration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.michael.sensorscope.domain.model.SensorBaseline
import com.michael.sensorscope.domain.model.SensorStatus
import com.michael.sensorscope.domain.repository.SensorRepository
import com.michael.sensorscope.domain.usecase.CaptureCalibrationBaselineUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class CalibrationUiState {
    object Loading : CalibrationUiState()
    data class Success(
        val baselines: List<SensorBaseline>,
        val availableSensors: List<SensorStatus>
    ) : CalibrationUiState()
}

class CalibrationViewModel(
    private val sensorRepository: SensorRepository,
    private val captureCalibrationBaselineUseCase: CaptureCalibrationBaselineUseCase
) : ViewModel() {

    private val _isCalibrating = MutableStateFlow<Int?>(null)
    val isCalibrating = _isCalibrating.asStateFlow()

    val uiState: StateFlow<CalibrationUiState> = combine(
        sensorRepository.observeBaselines(),
        sensorRepository.observeAllSensorHealth()
    ) { baselines, healthMap ->
        CalibrationUiState.Success(
            baselines = baselines,
            availableSensors = healthMap.values.toList()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CalibrationUiState.Loading
    )

    fun calibrateSensor(sensorType: Int) {
        viewModelScope.launch {
            _isCalibrating.value = sensorType
            try {
                captureCalibrationBaselineUseCase(sensorType)
            } catch (e: Exception) {
                // Safely dismiss error
            } finally {
                _isCalibrating.value = null
            }
        }
    }

    fun deleteBaseline(sensorType: Int) {
        viewModelScope.launch {
            sensorRepository.clearBaseline(sensorType)
        }
    }

    fun clearAllBaselines() {
        viewModelScope.launch {
            sensorRepository.clearAllBaselines()
        }
    }
}
