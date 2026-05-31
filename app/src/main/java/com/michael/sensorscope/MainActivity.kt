package com.michael.sensorscope

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import androidx.core.content.ContextCompat
import com.michael.sensorscope.core.ui.ViewModelFactory
import com.michael.sensorscope.feature.battery_telemetry.BatteryTelemetryScreen
import com.michael.sensorscope.feature.battery_telemetry.BatteryTelemetryViewModel
import com.michael.sensorscope.feature.calibration.CalibrationScreen
import com.michael.sensorscope.feature.calibration.CalibrationViewModel
import com.michael.sensorscope.feature.dashboard.DashboardScreen
import com.michael.sensorscope.feature.dashboard.DashboardViewModel
import com.michael.sensorscope.feature.dead_pixel.DeadPixelScreen
import com.michael.sensorscope.feature.report_preview.ReportPreviewScreen
import com.michael.sensorscope.feature.report_preview.ReportPreviewViewModel
import com.michael.sensorscope.feature.sensor_detail.SensorDetailScreen
import com.michael.sensorscope.feature.sensor_detail.SensorDetailViewModel
import com.michael.sensorscope.feature.thermal_stress.ThermalStressScreen
import com.michael.sensorscope.feature.thermal_stress.ThermalStressViewModel
import com.michael.sensorscope.ui.theme.MyApplicationTheme

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

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

@Composable
fun AppBottomNavigationBar(
    navItems: List<BottomNavItem>,
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier.testTag("app_navigation_bar")
    ) {
        navItems.forEach { item ->
            val isSelected = currentRoute == item.route
            NavigationBarItem(
                icon = { Icon(imageVector = item.icon, contentDescription = item.label) },
                label = if (isSelected) {
                    {
                        Text(
                            text = item.label,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    null
                },
                selected = isSelected,
                alwaysShowLabel = false,
                onClick = {
                    if (currentRoute != item.route) {
                        onNavigate(item.route)
                    }
                },
                modifier = Modifier.testTag("nav_item_${item.route}")
            )
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val app = application as SensorScopeApp
            val factory = ViewModelFactory(app)

            MyApplicationTheme {
                NotificationPermissionRequestEffect()

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
                            AppBottomNavigationBar(
                                navItems = listOf(
                                    BottomNavItem("Sensors", Icons.Default.Sensors, Screen.Dashboard),
                                    BottomNavItem("Screen", Icons.Default.AspectRatio, Screen.DeadPixel),
                                    BottomNavItem("Thermals", Icons.Default.Thermostat, Screen.ThermalStress),
                                    BottomNavItem("Power", Icons.Default.BatteryChargingFull, Screen.BatteryTelemetry),
                                    BottomNavItem("Offsets", Icons.Default.Tune, Screen.Calibration),
                                    BottomNavItem("Dossier", Icons.Default.Summarize, Screen.ReportPreview)
                                ),
                                currentRoute = currentRoute,
                                onNavigate = { route ->
                                    navController.navigate(route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
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

@Composable
private fun NotificationPermissionRequestEffect() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {}
    )
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(Unit) {
        val alreadyGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (!alreadyGranted) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
