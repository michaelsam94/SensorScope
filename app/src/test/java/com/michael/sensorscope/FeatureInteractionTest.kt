package com.michael.sensorscope

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.runtime.mutableStateOf
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.michael.sensorscope.domain.model.BatteryReading
import com.michael.sensorscope.domain.model.SensorBaseline
import com.michael.sensorscope.domain.model.SensorCategory
import com.michael.sensorscope.domain.model.SensorHealth
import com.michael.sensorscope.domain.model.SensorInfo
import com.michael.sensorscope.domain.model.SensorReading
import com.michael.sensorscope.domain.model.SensorStatus
import com.michael.sensorscope.domain.repository.BatteryRepository
import com.michael.sensorscope.domain.repository.SensorRepository
import com.michael.sensorscope.domain.usecase.CaptureCalibrationBaselineUseCase
import com.michael.sensorscope.domain.usecase.ObserveAllSensorHealthUseCase
import com.michael.sensorscope.domain.usecase.ObserveBatteryTelemetryUseCase
import com.michael.sensorscope.domain.usecase.ObserveSensorStreamUseCase
import com.michael.sensorscope.feature.battery_telemetry.BatteryTelemetryScreen
import com.michael.sensorscope.feature.battery_telemetry.BatteryTelemetryViewModel
import com.michael.sensorscope.feature.calibration.CalibrationScreen
import com.michael.sensorscope.feature.calibration.CalibrationViewModel
import com.michael.sensorscope.feature.dashboard.CategoryFilterRow
import com.michael.sensorscope.feature.dashboard.DashboardScreen
import com.michael.sensorscope.feature.dead_pixel.DeadPixelScreen
import com.michael.sensorscope.feature.sensor_detail.SensorDetailScreen
import com.michael.sensorscope.feature.sensor_detail.SensorDetailViewModel
import com.michael.sensorscope.feature.thermal_stress.StressControlCard
import com.michael.sensorscope.feature.thermal_stress.StressState
import com.michael.sensorscope.ui.theme.MyApplicationTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [35])
class FeatureInteractionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun dashboardCategoryFiltersScrollHorizontally() {
        composeTestRule.setContent {
            MyApplicationTheme {
                CategoryFilterRow(
                    selected = null,
                    onSelected = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("category_filter_row").assert(hasScrollAction())
    }

    @Test
    fun dashboard_filtersSensorsAndOpensSensorDetails() {
        val repository = FakeSensorRepository(
            statuses = listOf(
                sensorStatus(type = 1, name = "Accelerometer", category = SensorCategory.MOTION),
                sensorStatus(type = 2, name = "Thermometer", category = SensorCategory.ENVIRONMENT)
            )
        )
        val viewModel = com.michael.sensorscope.feature.dashboard.DashboardViewModel(
            observeAllSensorHealthUseCase = ObserveAllSensorHealthUseCase(repository),
            sensorRepository = repository
        )
        var openedSensorType: Int? = null

        composeTestRule.setContent {
            MyApplicationTheme {
                DashboardScreen(
                    viewModel = viewModel,
                    onSensorClick = { openedSensorType = it }
                )
            }
        }

        composeTestRule.onNodeWithText("Accelerometer").assertIsDisplayed()
        composeTestRule.onNodeWithText("ENVIRONMENT").assertIsEnabled().performClick()
        composeTestRule.onNodeWithText("Thermometer").assertIsDisplayed()
        composeTestRule.onNodeWithTag("sensor_card_2").assertIsEnabled().performClick()

        assertEquals(2, openedSensorType)
    }

    @Test
    fun sensorDetailShowsMetadataWhenSensorHasNoReadingsYet() {
        val repository = FakeSensorRepository(
            statuses = listOf(sensorStatus(type = 11, name = "Quiet Sensor", category = SensorCategory.UNKNOWN)),
            stream = emptyFlow()
        )
        val viewModel = SensorDetailViewModel(
            observeSensorStreamUseCase = ObserveSensorStreamUseCase(repository),
            sensorRepository = repository
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                SensorDetailScreen(
                    viewModel = viewModel,
                    sensorType = 11,
                    onBackClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Vendor Info: SensorScope Labs (v1)").assertIsDisplayed()
        composeTestRule.onNodeWithTag("live_sensor_canvas").assertIsDisplayed()
        composeTestRule.mainClock.advanceTimeBy(3_000)
        composeTestRule.waitUntil(timeoutMillis = 4_000) {
            composeTestRule.onAllNodesWithText("No live readings available from this sensor. Try moving the device or choose another sensor.").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun calibration_actionsInvokeRepositoryOperations() {
        val repository = FakeSensorRepository(
            statuses = listOf(sensorStatus(type = 7, name = "Gyroscope", category = SensorCategory.MOTION)),
            baselines = listOf(sensorBaseline(sensorType = 7, sensorName = "Gyroscope"))
        )
        val viewModel = CalibrationViewModel(
            sensorRepository = repository,
            captureCalibrationBaselineUseCase = CaptureCalibrationBaselineUseCase(repository)
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                CalibrationScreen(viewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithText("Wipe All").assertIsEnabled().performClick()
        composeTestRule.waitUntil { repository.clearAllCount == 1 }
        composeTestRule.onNodeWithTag("sweep_calibrate_7").assertIsEnabled().performClick()
        composeTestRule.waitUntil { repository.capturedSensorTypes.isNotEmpty() }
        composeTestRule.onNodeWithContentDescription("Reset baseline").assertIsEnabled().performClick()
        composeTestRule.waitUntil { repository.clearedSensorTypes.isNotEmpty() }

        assertEquals(listOf(7), repository.capturedSensorTypes)
        assertEquals(listOf(7), repository.clearedSensorTypes)
    }

    @Test
    fun batteryTelemetryShowsAnomalyBannerForUnsafeReading() {
        val viewModel = BatteryTelemetryViewModel(
            ObserveBatteryTelemetryUseCase(
                FakeBatteryRepository(
                    BatteryReading(
                        level = 22,
                        health = 2,
                        voltage = 3300,
                        currentMicroAmp = -420000,
                        temperature = 44.5f,
                        technology = "Li-ion",
                        isCharging = false
                    )
                )
            )
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                BatteryTelemetryScreen(viewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithTag("battery_anomaly_banner").assertIsDisplayed()
        composeTestRule.onNodeWithTag("battery_percent_label").assertIsDisplayed()
        composeTestRule.onNodeWithText("22%").assertIsDisplayed()
    }

    @Test
    fun deadPixelPaletteControlsAreClickable() {
        var backClicks = 0

        composeTestRule.setContent {
            MyApplicationTheme {
                DeadPixelScreen(onBackClick = { backClicks++ })
            }
        }

        composeTestRule.onNodeWithTag("palette_control_bar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("toggle_brush_btn").assertIsEnabled().performClick()
        composeTestRule.onNodeWithTag("color_pill_2").assertIsEnabled().performClick()
        composeTestRule.onNodeWithContentDescription("Return").assertIsEnabled().performClick()

        assertEquals(1, backClicks)
    }

    @Test
    fun stressControlCardExposesStartAndStopActions() {
        var startClicks = 0
        var stopClicks = 0
        val stressState = mutableStateOf<StressState>(StressState.Idle)

        composeTestRule.setContent {
            MyApplicationTheme {
                StressControlCard(
                    state = stressState.value,
                    onStartClick = { startClicks++ },
                    onStopClick = { stopClicks++ }
                )
            }
        }

        composeTestRule.onNodeWithTag("launch_stress_btn").assertIsEnabled().performClick()

        composeTestRule.runOnIdle {
            stressState.value = StressState.Running(
                elapsedMs = 12_000L,
                maxCpuTemp = 39f,
                maxBatteryTemp = 35f,
                currentCpuTemp = 38f,
                currentBatteryTemp = 34f,
                progress = 0.4f
            )
        }

        composeTestRule.onNodeWithTag("abort_stress_btn").assertIsEnabled().performClick()

        assertEquals(1, startClicks)
        assertEquals(1, stopClicks)
    }

    private fun sensorInfo(
        type: Int,
        name: String,
        category: SensorCategory
    ) = SensorInfo(
        type = type,
        name = name,
        vendor = "SensorScope Labs",
        version = 1,
        maximumRange = 10f,
        resolution = 0.01f,
        power = 0.5f,
        minDelay = 1000,
        category = category
    )

    private fun sensorStatus(
        type: Int,
        name: String,
        category: SensorCategory
    ) = SensorStatus(
        sensorInfo = sensorInfo(type = type, name = name, category = category),
        health = SensorHealth.OK,
        baselineX = if (type == 7) 0.1f else 0f
    )

    private fun sensorBaseline(sensorType: Int, sensorName: String) = SensorBaseline(
        sensorType = sensorType,
        sensorName = sensorName,
        xOffset = 0.1f,
        yOffset = 0.2f,
        zOffset = 0.3f,
        timestamp = 1L
    )

    private class FakeBatteryRepository(
        reading: BatteryReading
    ) : BatteryRepository {
        private val readingFlow = MutableStateFlow(reading)

        override fun observeBatteryTelemetry(): Flow<BatteryReading> = readingFlow
    }

    private class FakeSensorRepository(
        statuses: List<SensorStatus>,
        baselines: List<SensorBaseline> = emptyList(),
        private val stream: Flow<SensorReading> = MutableStateFlow(SensorReading(timestamp = 1L, values = listOf(1f, 2f, 3f)))
    ) : SensorRepository {
        private val statusesFlow = MutableStateFlow(statuses.associateBy { it.sensorInfo.type })
        private val baselinesFlow = MutableStateFlow(baselines)

        val capturedSensorTypes = mutableListOf<Int>()
        val clearedSensorTypes = mutableListOf<Int>()
        var clearAllCount = 0

        override fun getAvailableSensors(): List<SensorInfo> =
            statusesFlow.value.values.map { it.sensorInfo }

        override fun observeAllSensorHealth(): Flow<Map<Int, SensorStatus>> = statusesFlow

        override fun observeSensorStream(sensorType: Int): Flow<SensorReading> = stream

        override fun observeBaselines(): Flow<List<SensorBaseline>> = baselinesFlow

        override suspend fun getBaselineForSensor(sensorType: Int): SensorBaseline? =
            baselinesFlow.value.firstOrNull { it.sensorType == sensorType }

        override suspend fun saveBaseline(baseline: SensorBaseline) {
            baselinesFlow.value = baselinesFlow.value + baseline
        }

        override suspend fun clearBaseline(sensorType: Int) {
            clearedSensorTypes += sensorType
            baselinesFlow.value = baselinesFlow.value.filterNot { it.sensorType == sensorType }
        }

        override suspend fun clearAllBaselines() {
            clearAllCount++
            baselinesFlow.value = emptyList()
        }

        override suspend fun captureBaseline(sensorType: Int): SensorBaseline {
            capturedSensorTypes += sensorType
            val status = statusesFlow.value.getValue(sensorType)
            val baseline = SensorBaseline(
                sensorType = sensorType,
                sensorName = status.sensorInfo.name,
                xOffset = 0.1f,
                yOffset = 0.2f,
                zOffset = 0.3f,
                timestamp = 1L
            )
            saveBaseline(baseline)
            return baseline
        }
    }
}
