package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.core.ui.ViewModelFactory
import com.example.feature.battery_telemetry.BatteryTelemetryScreen
import com.example.feature.battery_telemetry.BatteryTelemetryViewModel
import com.example.feature.calibration.CalibrationScreen
import com.example.feature.calibration.CalibrationViewModel
import com.example.feature.dashboard.DashboardScreen
import com.example.feature.dashboard.DashboardViewModel
import com.example.feature.dead_pixel.DeadPixelScreen
import com.example.feature.report_preview.ReportPreviewScreen
import com.example.feature.report_preview.ReportPreviewViewModel
import com.example.feature.sensor_detail.SensorDetailScreen
import com.example.feature.sensor_detail.SensorDetailViewModel
import com.example.feature.thermal_stress.ThermalStressScreen
import com.example.feature.thermal_stress.ThermalStressViewModel
import com.example.ui.theme.MyApplicationTheme

object Screen {
    const val Dashboard = "dashboard"
    const val SensorDetail = "sensor_detail/{sensorType}"
    const val DeadPixel = "dead_pixel"
    const val ThermalStress = "thermal_stress"
    const val BatteryTelemetry = "battery_telemetry"
    const val Calibration = "calibration"
    const val ReportPreview = "report_preview"

    fun sensorDetailRoute(sensorType: Int) = "sensor_detail/$sensorType"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val app = application as SensorScopeApp
            val factory = ViewModelFactory(app)

            MyApplicationTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                // Hide standard navigation bar during immersive dead-pixel inspections or deep plots
                val showBottomBar = currentRoute != null && 
                        currentRoute != Screen.DeadPixel && 
                        !currentRoute.startsWith("sensor_detail")

                Scaffold(
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar(
                                modifier = Modifier.testTag("app_navigation_bar")
                            ) {
                                val navItems = listOf(
                                    Triple("Sensors", Icons.Default.Sensors, Screen.Dashboard),
                                    Triple("Screen", Icons.Default.AspectRatio, Screen.DeadPixel),
                                    Triple("Thermals", Icons.Default.Thermostat, Screen.ThermalStress),
                                    Triple("Power", Icons.Default.BatteryChargingFull, Screen.BatteryTelemetry),
                                    Triple("Offsets", Icons.Default.Tune, Screen.Calibration),
                                    Triple("Dossier", Icons.Default.Summarize, Screen.ReportPreview)
                                )

                                navItems.forEach { (label, icon, route) ->
                                    val isSelected = currentRoute == route
                                    NavigationBarItem(
                                        icon = { Icon(imageVector = icon, contentDescription = label) },
                                        label = { Text(label, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) },
                                        selected = isSelected,
                                        onClick = {
                                            if (currentRoute != route) {
                                                navController.navigate(route) {
                                                    popUpTo(navController.graph.findStartDestination().id) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        },
                                        modifier = Modifier.testTag("nav_item_$route")
                                    )
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Dashboard,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(Screen.Dashboard) {
                            val dashboardVm: DashboardViewModel = viewModel(factory = factory)
                            DashboardScreen(
                                viewModel = dashboardVm,
                                onSensorClick = { sensorType ->
                                    navController.navigate(Screen.sensorDetailRoute(sensorType))
                                }
                            )
                        }

                        composable(
                            route = Screen.SensorDetail,
                            arguments = listOf(navArgument("sensorType") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val sensorType = backStackEntry.arguments?.getInt("sensorType") ?: -1
                            val detailVm: SensorDetailViewModel = viewModel(factory = factory)
                            SensorDetailScreen(
                                viewModel = detailVm,
                                sensorType = sensorType,
                                onBackClick = { navController.popBackStack() }
                            )
                        }

                        composable(Screen.DeadPixel) {
                            DeadPixelScreen(
                                onBackClick = { navController.popBackStack() }
                            )
                        }

                        composable(Screen.ThermalStress) {
                            val thermalVm: ThermalStressViewModel = viewModel(factory = factory)
                            ThermalStressScreen(
                                viewModel = thermalVm
                            )
                        }

                        composable(Screen.BatteryTelemetry) {
                            val batteryVm: BatteryTelemetryViewModel = viewModel(factory = factory)
                            BatteryTelemetryScreen(
                                viewModel = batteryVm
                            )
                        }

                        composable(Screen.Calibration) {
                            val calibrationVm: CalibrationViewModel = viewModel(factory = factory)
                            CalibrationScreen(
                                viewModel = calibrationVm
                            )
                        }

                        composable(Screen.ReportPreview) {
                            val reportVm: ReportPreviewViewModel = viewModel(factory = factory)
                            ReportPreviewScreen(
                                viewModel = reportVm
                            )
                        }
                    }
                }
            }
        }
    }
}
