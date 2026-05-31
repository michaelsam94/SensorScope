package com.michael.sensorscope.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.michael.sensorscope.domain.model.SensorCategory
import com.michael.sensorscope.domain.model.SensorStatus
import com.michael.sensorscope.domain.repository.SensorRepository
import com.michael.sensorscope.domain.usecase.ObserveAllSensorHealthUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

sealed class DashboardUiState {
    object Loading : DashboardUiState()
    data class Success(
        val sensors: List<SensorStatus>,
        val activeCategory: SensorCategory?
    ) : DashboardUiState()
}

class DashboardViewModel(
    observeAllSensorHealthUseCase: ObserveAllSensorHealthUseCase,
    private val sensorRepository: SensorRepository
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow<SensorCategory?>(null)
    val selectedCategory: StateFlow<SensorCategory?> = _selectedCategory

    val uiState: StateFlow<DashboardUiState> = combine(
        observeAllSensorHealthUseCase(),
        _selectedCategory
    ) { statusMap, category ->
        val sensorList = statusMap.values.toList()
        val filtered = if (category == null) {
            sensorList
        } else {
            sensorList.filter { it.sensorInfo.category == category }
        }
        DashboardUiState.Success(filtered, category)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState.Loading
    )

    fun selectCategory(category: SensorCategory?) {
        _selectedCategory.value = category
    }
}
