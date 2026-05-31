package com.michael.sensorscope.playstore

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.michael.sensorscope.domain.model.SensorCategory
import com.michael.sensorscope.domain.usecase.ObserveAllSensorHealthUseCase
import com.michael.sensorscope.domain.usecase.ObserveBatteryTelemetryUseCase
import com.michael.sensorscope.domain.usecase.ObserveSensorStreamUseCase
import com.michael.sensorscope.feature.battery_telemetry.BatteryTelemetryScreen
import com.michael.sensorscope.feature.battery_telemetry.BatteryTelemetryViewModel
import com.michael.sensorscope.feature.dashboard.DashboardScreen
import com.michael.sensorscope.feature.dashboard.DashboardViewModel
import com.michael.sensorscope.feature.sensor_detail.SensorDetailScreen
import com.michael.sensorscope.feature.sensor_detail.SensorDetailViewModel
import com.michael.sensorscope.ui.theme.MyApplicationTheme

enum class PlayStoreScene {
    Dashboard,
    Filters,
    Battery,
    Detail,
}

@Composable
fun PlayStoreScreenshotFrame(scene: PlayStoreScene) {
    val sensorRepository = remember { FakeSensorRepository() }
    val batteryRepository = remember { FakeBatteryRepository() }

    MyApplicationTheme(darkTheme = false, dynamicColor = false) {
        when (scene) {
            PlayStoreScene.Dashboard -> {
                val viewModel = remember {
                    DashboardViewModel(ObserveAllSensorHealthUseCase(sensorRepository), sensorRepository)
                }
                DashboardScreen(viewModel = viewModel, onSensorClick = {}, modifier = Modifier.fillMaxSize())
            }
            PlayStoreScene.Filters -> {
                val viewModel = remember {
                    DashboardViewModel(ObserveAllSensorHealthUseCase(sensorRepository), sensorRepository)
                }
                LaunchedEffect(Unit) {
                    viewModel.selectCategory(SensorCategory.ENVIRONMENT)
                }
                DashboardScreen(viewModel = viewModel, onSensorClick = {}, modifier = Modifier.fillMaxSize())
            }
            PlayStoreScene.Battery -> {
                val viewModel = remember {
                    BatteryTelemetryViewModel(ObserveBatteryTelemetryUseCase(batteryRepository))
                }
                BatteryTelemetryScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize())
            }
            PlayStoreScene.Detail -> {
                val viewModel = remember {
                    SensorDetailViewModel(
                        ObserveSensorStreamUseCase(sensorRepository),
                        sensorRepository,
                    )
                }
                SensorDetailScreen(
                    viewModel = viewModel,
                    sensorType = 1,
                    onBackClick = {},
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
