package com.michael.sensorscope.feature.battery_telemetry

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.michael.sensorscope.domain.model.BatteryReading
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryTelemetryScreen(
    viewModel: BatteryTelemetryViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Battery Diagnostics", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) },
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
                "Power Quality & Telemetry",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Diagnostic monitor analyzing cell voltage levels, thermal parameters, and real-time current draw.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            when (val state = uiState) {
                is BatteryUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is BatteryUiState.Success -> {
                    val reading = state.reading
                    
                    // Display Anomaly alert banner if anomalies are detected
                    if (reading.isAnomaly) {
                        AnomalyAlertCard(reading = reading)
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Battery level status tile
                    BatteryGaugeCard(reading = reading)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Grid-like list of telemetry readings
                    TelemetryMetricsSection(reading = reading)
                }
            }
        }
    }
}

@Composable
fun AnomalyAlertCard(reading: BatteryReading) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("battery_anomaly_banner"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "HARDWARE ANOMALY DETECTED",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = if (reading.temperature > 40f) {
                        "CELL TEMPERATURE EXCEEDS CRITICAL threshold (currently ${reading.temperature}°C)"
                    } else if (reading.voltage < 3400) {
                        "CELL VOLTAGE DROP DETECTED (${reading.voltage / 1000f} V). Risk of unexpected collapse."
                    } else {
                        "CELL HEALTH DEGRADED DETECTED. Replacement advised."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
fun BatteryGaugeCard(reading: BatteryReading) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = if (reading.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
                contentDescription = null,
                tint = if (reading.isCharging) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "${reading.level}%",
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 44.sp),
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.testTag("battery_percent_label")
            )

            Text(
                text = if (reading.isCharging) "Power Source Connected (Charging)" else "Discharging from Battery",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun TelemetryMetricsSection(reading: BatteryReading) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Diagnostic Metrics", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            TelemetryItemRow(
                label = "Battery Health Status",
                value = reading.healthString,
                color = if (reading.healthString == "Good") Color(0xFF4CAF50) else Color.Red
            )
            
            TelemetryItemRow(
                label = "Cell Voltage Indicator",
                value = String.format(Locale.US, "%.3f V", reading.voltage / 1000f),
                color = if (reading.voltage < 3400) Color.Red else MaterialTheme.colorScheme.onSurface
            )

            TelemetryItemRow(
                label = "Dynamic Current Flow",
                value = "${reading.currentMicroAmp} µA",
                color = MaterialTheme.colorScheme.onSurface
            )

            TelemetryItemRow(
                label = "Internal Temperature",
                value = "${reading.temperature} °C",
                color = if (reading.temperature > 40f) Color.Red else MaterialTheme.colorScheme.onSurface
            )

            TelemetryItemRow(
                label = "Cell Technology Structure",
                value = reading.technology,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun TelemetryItemRow(
    label: String,
    value: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = color
        )
    }
}
