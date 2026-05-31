package com.michael.sensorscope.feature.battery_telemetry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.michael.sensorscope.domain.model.BatteryReading
import com.michael.sensorscope.domain.usecase.ObserveBatteryTelemetryUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed class BatteryUiState {
    object Loading : BatteryUiState()
    data class Success(val reading: BatteryReading) : BatteryUiState()
}

class BatteryTelemetryViewModel(
    observeBatteryTelemetryUseCase: ObserveBatteryTelemetryUseCase
) : ViewModel() {

    val uiState: StateFlow<BatteryUiState> = observeBatteryTelemetryUseCase()
        .map { BatteryUiState.Success(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = BatteryUiState.Loading
        )
}
