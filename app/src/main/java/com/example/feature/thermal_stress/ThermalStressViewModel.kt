package com.example.feature.thermal_stress

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.ThermalStressLog
import com.example.domain.repository.ThermalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ThermalStressViewModel(
    private val context: Context,
    private val thermalRepository: ThermalRepository
) : ViewModel() {

    private val _isBound = MutableStateFlow(false)
    val isBound = _isBound.asStateFlow()

    private val _stressState = MutableStateFlow<StressState>(StressState.Idle)
    val stressState: StateFlow<StressState> = _stressState.asStateFlow()

    private var thermalService: ThermalStressService? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName?, serviceBinder: IBinder?) {
            val binder = serviceBinder as? ThermalStressService.ThermalStressBinder
            thermalService = binder?.getService()
            _isBound.value = true
            
            thermalService?.stressState?.onEach { state ->
                _stressState.value = state
            }?.launchIn(viewModelScope)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            thermalService = null
            _isBound.value = false
            _stressState.value = StressState.Idle
        }
    }

    val logHistory: StateFlow<List<ThermalStressLog>> = thermalRepository.observeStressLogs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        bindToService()
    }

    private fun bindToService() {
        val intent = Intent(context, ThermalStressService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    fun startStressTest() {
        val intent = Intent(context, ThermalStressService::class.java).apply {
            action = ThermalStressService.ACTION_START_STRESS
        }
        context.startService(intent)
        thermalService?.startStressTest()
    }

    fun stopStressTest() {
        thermalService?.stopStressTest(false)
    }

    fun clearHistory() {
        viewModelScope.launch {
            thermalRepository.clearStressLogs()
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (_isBound.value) {
            try {
                context.unbindService(serviceConnection)
            } catch (e: Exception) {}
            _isBound.value = false
        }
    }
}
