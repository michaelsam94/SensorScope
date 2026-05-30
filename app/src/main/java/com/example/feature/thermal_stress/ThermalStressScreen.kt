package com.example.feature.thermal_stress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.ThermalStressLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThermalStressScreen(
    viewModel: ThermalStressViewModel,
    modifier: Modifier = Modifier
) {
    val stressState by viewModel.stressState.collectAsStateWithLifecycle()
    val logHistory by viewModel.logHistory.collectAsStateWithLifecycle()

    var showWarningDialog by remember { mutableStateOf(false) }

    // Plot tracking array lists
    val tempHistory = remember { mutableStateListOf<Pair<Float, Float>>() } // CPU, Battery

    // Sync live tracking dots with active run
    LaunchedEffect(stressState) {
        val state = stressState
        if (state is StressState.Running) {
            tempHistory.add(Pair(state.currentCpuTemp, state.currentBatteryTemp))
        } else if (state is StressState.Idle) {
            tempHistory.clear()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thermal Stress Sandbox", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                "Thermals & Throttling Diagnostic",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Sandbox runs intensive parallel thread cycles to heat CPU, validating sensor throttling limits and thermal control shutoff triggers.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Current state dashboard tile
            StressControlCard(
                state = stressState,
                onStartClick = { showWarningDialog = true },
                onStopClick = { viewModel.stopStressTest() }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Real-Time Temperature Canvas Plot
            if (stressState is StressState.Running || tempHistory.isNotEmpty()) {
                Text(
                    "Sandbox Temp Plot (°C)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LiveThermalPlot(
                    tempHistory = tempHistory,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .testTag("thermal_canvas")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Legends row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(modifier = Modifier.size(12.dp).padding(2.dp).align(Alignment.CenterVertically)) {
                        Canvas(modifier = Modifier.fillMaxSize()) { drawLabelColor(Color.Red) }
                    }
                    Text("CPU Core Temp", fontSize = 11.sp, color = Color.Red, modifier = Modifier.padding(start = 4.dp, end = 16.dp))
                    
                    Box(modifier = Modifier.size(12.dp).padding(2.dp).align(Alignment.CenterVertically)) {
                        Canvas(modifier = Modifier.fillMaxSize()) { drawLabelColor(Color(0xFFFF9800)) }
                    }
                    Text("Battery Cell", fontSize = 11.sp, color = Color(0xFFFF9800), modifier = Modifier.padding(start = 4.dp))
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Results summary card
            if (stressState is StressState.Completed) {
                val log = (stressState as StressState.Completed).log
                StressResultCard(log = log)
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Historical sandboxes logs grid list
            HistoricalLogsSection(
                logs = logHistory,
                onClearHistory = { viewModel.clearHistory() }
            )
        }
    }

    if (showWarningDialog) {
        AlertDialog(
            onDismissRequest = { showWarningDialog = false },
            title = { Text("Thermal War Sandbox Warning", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This diagnostic execution will intentionally stress your CPU cores, consuming high power and generating safe internal heat.\n\n" +
                    "• The test lasts for 30 seconds maximum.\n" +
                    "• Features an active safety ceiling at 45.0 °C battery temp.\n" +
                    "• It is fully controlled, safe, and can be aborted manually at any second."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showWarningDialog = false
                        viewModel.startStressTest()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_stress_launch_btn")
                ) {
                    Text("Enable Warm Sandbox")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showWarningDialog = false }) {
                    Text("Dismiss")
                }
            },
            modifier = Modifier.testTag("safety_dialog")
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLabelColor(color: Color) {
    drawRect(color = color)
}

@Composable
fun StressControlCard(
    state: StressState,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            when (state) {
                is StressState.Idle -> {
                    Text("Status: Sandbox Ready", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onStartClick,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth().testTag("launch_stress_btn")
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Stress Sandbox")
                    }
                }
                is StressState.Running -> {
                    Text("Status: STRESSING HARDWARE", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("CPU Core Celsius: ${String.format(Locale.US, "%.1f", state.currentCpuTemp)} °C", fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                    Text("Battery Cell: ${String.format(Locale.US, "%.1f", state.currentBatteryTemp)} °C", fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    LinearProgressIndicator(
                        progress = { state.progress },
                        modifier = Modifier.fillMaxWidth().height(10.dp),
                        color = MaterialTheme.colorScheme.error,
                        trackColor = MaterialTheme.colorScheme.errorContainer
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onStopClick,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth().testTag("abort_stress_btn")
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Abort Sandbox Stress")
                    }
                }
                is StressState.Completed -> {
                    Text("Status: TEST COMPLETED", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onStartClick,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Execute Again")
                    }
                }
            }
        }
    }
}

@Composable
fun LiveThermalPlot(
    tempHistory: List<Pair<Float, Float>>,
    modifier: Modifier = Modifier
) {
    val axisColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            val width = size.width
            val height = size.height

            // Fixed bounds for temperature plot (e.g. 25°C to 50°C)
            val minVal = 20f
            val maxVal = 55f

            // Draw horizontal references
            for (tempLevel in listOf(25, 35, 45, 55)) {
                val y = height - (height * (tempLevel - minVal) / (maxVal - minVal))
                drawLine(color = gridColor, start = Offset(0f, y), end = Offset(width, y), strokeWidth = 1f)
            }

            if (tempHistory.size > 1) {
                val totalPoints = tempHistory.size
                val pathCpu = Path()
                val pathBat = Path()

                tempHistory.forEachIndexed { idx, pair ->
                    val rx = width * (idx.toFloat() / (totalPoints - 1).toFloat())
                    
                    val ryCpu = height - (height * (pair.first - minVal) / (maxVal - minVal))
                    val ryBat = height - (height * (pair.second - minVal) / (maxVal - minVal))

                    if (idx == 0) {
                        pathCpu.moveTo(rx, ryCpu)
                        pathBat.moveTo(rx, ryBat)
                    } else {
                        pathCpu.lineTo(rx, ryCpu)
                        pathBat.lineTo(rx, ryBat)
                    }
                }

                drawPath(path = pathCpu, color = Color.Red, style = Stroke(width = 4f))
                drawPath(path = pathBat, color = Color(0xFFFF9800), style = Stroke(width = 4f))
            }
        }
    }
}

@Composable
fun StressResultCard(log: ThermalStressLog) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Summarize, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Diagnostics Report Summary", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(10.dp))

            Text("Max CPU Temperature: ${String.format(Locale.US, "%.1f", log.maxCpuTemp)} °C", color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text("Max Battery Temperature: ${String.format(Locale.US, "%.1f", log.maxBatteryTemp)} °C", color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text("Test Duration Elapsed: ${log.durationMs / 1000} seconds", color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text("Safety Trigger Trip: ${if (log.safetyStopTriggered) "YES (45°C safety active)" else "NO"}", color = MaterialTheme.colorScheme.onPrimaryContainer)
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = log.healthAssessment, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
fun HistoricalLogsSection(
    logs: List<ThermalStressLog>,
    onClearHistory: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Stress Log Run History", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        if (logs.isNotEmpty()) {
            TextButton(onClick = onClearHistory) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Clear History")
            }
        }
    }

    if (logs.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No sandbox tests logged yet.", color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.bodyMedium)
        }
    } else {
        logs.forEach { log ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                        Text(
                            text = dateFormat.format(Date(log.timestamp)),
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (log.safetyStopTriggered) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = MaterialTheme.shapes.extraSmall
                            ) {
                                Text("SAFETY STOP", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(2.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text("Max CPU: ${String.format(Locale.US, "%.1f", log.maxCpuTemp)} °C  |  Max Battery: ${String.format(Locale.US, "%.1f", log.maxBatteryTemp)} °C", style = MaterialTheme.typography.bodySmall)
                    Text("Duration: ${log.durationMs / 1000}s", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}
