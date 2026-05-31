package com.michael.sensorscope.feature.sensor_detail

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.michael.sensorscope.domain.model.SensorInfo
import com.michael.sensorscope.domain.model.SensorReading
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorDetailScreen(
    viewModel: SensorDetailViewModel,
    sensorType: Int,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Initialize with sensorType on launch or when type shifts
    LaunchedEffect(sensorType) {
        viewModel.initialize(sensorType)
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isCalibrating by viewModel.isCalibrating.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState is SensorDetailUiState.Success) {
                            (uiState as SensorDetailUiState.Success).sensor.name
                        } else {
                            "Sensor Real-Time Plot"
                        },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        when (val state = uiState) {
            is SensorDetailUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is SensorDetailUiState.SensorUnavailable -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Sensor unavailable on this hardware.")
                }
            }
            is SensorDetailUiState.Success -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // Title and category description
                    Text(
                        text = "Vendor Info: ${state.sensor.vendor} (v${state.sensor.version})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Real-Time Canvas Graph
                    LiveSensorGraph(
                        readings = state.readings,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .testTag("live_sensor_canvas")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Raw Value Readouts
                    val latestReading = state.readings.lastOrNull()
                    RawReadingReadouts(
                        reading = latestReading,
                        calX = state.calX,
                        calY = state.calY,
                        calZ = state.calZ,
                        streamUnavailable = state.streamUnavailable
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Technical Metadata Accordion
                    SensorMetadataCard(sensor = state.sensor)

                    Spacer(modifier = Modifier.height(24.dp))

                    // Interactive Controls Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { viewModel.calibrateSensor(sensorType) },
                            enabled = !isCalibrating,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("calibrate_button")
                        ) {
                            if (isCalibrating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = MaterialTheme.colorScheme.onSecondary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Averaging...", fontSize = 13.sp)
                            } else {
                                Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Zero Calibrate", fontSize = 13.sp)
                            }
                        }

                        if (state.calX != 0f || state.calY != 0f || state.calZ != 0f) {
                            OutlinedButton(
                                onClick = { viewModel.clearCalibration(sensorType) },
                                modifier = Modifier.weight(0.8f)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reset")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val file = viewModel.exportCsvSnapshot(context, sensorType)
                            if (file != null) {
                                try {
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "com.michael.sensorscope.provider",
                                        file
                                    )
                                    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        type = "text/csv"
                                        putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Sensor Snapshot CSV"))
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Saved to ${file.absolutePath}", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                Toast.makeText(context, "No readings to export yet", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_csv_btn")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export 5s CSV Snapshot")
                    }
                }
            }
        }
    }
}

@Composable
fun LiveSensorGraph(
    readings: List<SensorReading>,
    modifier: Modifier = Modifier
) {
    val axisColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            val width = size.width
            val height = size.height

            // Calculate min/max bounds for responsive scale
            val allValues = readings.flatMap { it.values }
            var minVal = allValues.minOrNull() ?: -9.8f
            var maxVal = allValues.maxOrNull() ?: 9.8f

            if (maxVal - minVal < 1f) {
                minVal -= 1f
                maxVal += 1f
            } else {
                val pad = (maxVal - minVal) * 0.15f
                minVal -= pad
                maxVal += pad
            }

            // Draw clean background box
            drawRect(color = surfaceColor)

            // Draw grid lines
            val horizontalLines = 4
            for (i in 0..horizontalLines) {
                val y = height * i / horizontalLines
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1f
                )
            }

            val verticalLines = 8
            for (i in 0..verticalLines) {
                val x = width * i / verticalLines
                drawLine(
                    color = gridColor,
                    start = Offset(x, 0f),
                    end = Offset(x, height),
                    strokeWidth = 1f
                )
            }

            // Draw baseline reference line (where value is 0)
            if (minVal < 0f && maxVal > 0f) {
                val zeroY = height - (height * (0f - minVal) / (maxVal - minVal))
                drawLine(
                    color = axisColor,
                    start = Offset(0f, zeroY),
                    end = Offset(width, zeroY),
                    strokeWidth = 2f
                )
            }

            // Draw reading paths
            if (readings.size > 1) {
                val pointCount = readings.size
                val numAxes = readings.firstOrNull()?.values?.size ?: 0

                for (axisIndex in 0 until numAxes) {
                    val path = Path()
                    val strokeColor = when (axisIndex) {
                        0 -> Color.Red     // X
                        1 -> Color(0xFF4CAF50) // Y (Green)
                        2 -> Color(0xFF2196F3) // Z (Blue)
                        else -> Color.Cyan
                    }

                    readings.forEachIndexed { pointIndex, reading ->
                        val value = reading.values.getOrNull(axisIndex) ?: 0f
                        val rx = width * (pointIndex.toFloat() / (pointCount - 1).toFloat())
                        val ry = height - (height * (value - minVal) / (maxVal - minVal))

                        if (pointIndex == 0) {
                            path.moveTo(rx, ry)
                        } else {
                            path.lineTo(rx, ry)
                        }
                    }

                    drawPath(
                        path = path,
                        color = strokeColor,
                        style = Stroke(width = 3.5f)
                    )
                }
            }
        }
    }
}

@Composable
fun RawReadingReadouts(
    reading: SensorReading?,
    calX: Float,
    calY: Float,
    calZ: Float,
    streamUnavailable: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Telemetry Output (Calibrated)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(10.dp))

            if (reading == null) {
                val message = if (streamUnavailable) {
                    "No live readings available from this sensor. Try moving the device or choose another sensor."
                } else {
                    "Acquiring sensor stream signal..."
                }
                Text(message, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
            } else {
                val valueSize = reading.values.size
                if (valueSize >= 3) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Axis X (Pitch)", color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = String.format(Locale.US, "%.5f", reading.x),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                modifier = Modifier.testTag("raw_x_val")
                            )
                            if (calX != 0f) {
                                Text("offset: ${String.format(Locale.US, "%.3f", calX)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                        Column {
                            Text("Axis Y (Roll)", color = Color(0xFF4CAF50), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = String.format(Locale.US, "%.5f", reading.y),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                modifier = Modifier.testTag("raw_y_val")
                            )
                            if (calY != 0f) {
                                Text("offset: ${String.format(Locale.US, "%.3f", calY)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                        Column {
                            Text("Axis Z (Yaw)", color = Color(0xFF2196F3), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = String.format(Locale.US, "%.5f", reading.z),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                modifier = Modifier.testTag("raw_z_val")
                            )
                            if (calZ != 0f) {
                                Text("offset: ${String.format(Locale.US, "%.3f", calZ)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                } else {
                    // Single axis, e.g. Ambient Light, Barometer, Temperature
                    val rawVal = reading.values.firstOrNull() ?: 0f
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Reading Value:", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = String.format(Locale.US, "%.4f", rawVal),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 26.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("single_raw_val")
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SensorMetadataCard(sensor: SensorInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Hardware Specification Specs",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(10.dp))

            MetadataSpecRow("Maximum Range", "${sensor.maximumRange}")
            MetadataSpecRow("Resolution Precision", "${sensor.resolution}")
            MetadataSpecRow("Power Draw", "${sensor.power} mA")
            MetadataSpecRow("Min Operational Delay", "${sensor.minDelay} µs")
            MetadataSpecRow("Diagnostic Category", sensor.category.name)
        }
    }
}

@Composable
fun MetadataSpecRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}
