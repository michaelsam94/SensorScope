package com.michael.sensorscope.feature.calibration

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.michael.sensorscope.domain.model.SensorBaseline
import com.michael.sensorscope.domain.model.SensorStatus
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalibrationScreen(
    viewModel: CalibrationViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isCalibratingSensorType by viewModel.isCalibrating.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sensor Calibration", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) },
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
                "Static Offset Calibration",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Zero calibration records resting baseline offsets, eliminating internal sensor drifting and error anomalies on this hardware unit.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            when (val state = uiState) {
                is CalibrationUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is CalibrationUiState.Success -> {
                    if (state.baselines.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Active Baselines (${state.baselines.size})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            TextButton(onClick = { viewModel.clearAllBaselines() }) {
                                Icon(Icons.Default.DeleteForever, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Wipe All")
                            }
                        }

                        // Display list of existing baselines
                        state.baselines.forEach { baseline ->
                            BaselineLogCard(
                                baseline = baseline,
                                onDeleteClick = { viewModel.deleteBaseline(baseline.sensorType) }
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    Text("Sensor Zero-Calibration Panel", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 8.dp))
                    
                    state.availableSensors.forEach { status ->
                        val isThisCalibrating = isCalibratingSensorType == status.sensorInfo.type
                        
                        CalibrationRowItem(
                            status = status,
                            isCalibrating = isThisCalibrating,
                            onCalibrateClick = { viewModel.calibrateSensor(status.sensorInfo.type) },
                            onResetClick = { viewModel.deleteBaseline(status.sensorInfo.type) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BaselineLogCard(
    baseline: SensorBaseline,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = baseline.sensorName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                IconButton(onClick = onDeleteClick, modifier = Modifier.size(24.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Wipe", modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(text = "cal_X: ${String.format(Locale.US, "%.4f", baseline.xOffset)}", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                Text(text = "cal_Y: ${String.format(Locale.US, "%.4f", baseline.yOffset)}", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                Text(text = "cal_Z: ${String.format(Locale.US, "%.4f", baseline.zOffset)}", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun CalibrationRowItem(
    status: SensorStatus,
    isCalibrating: Boolean,
    onCalibrateClick: () -> Unit,
    onResetClick: () -> Unit
) {
    val hasCalibration = status.baselineX != 0f || status.baselineY != 0f || status.baselineZ != 0f
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = status.sensorInfo.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text(text = "Vendor: ${status.sensorInfo.vendor}", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                
                if (hasCalibration) {
                    Text(
                        text = "Calibrated static offsets active",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (isCalibrating) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                } else {
                    Button(
                        onClick = onCalibrateClick,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(32.dp).testTag("sweep_calibrate_${status.sensorInfo.type}")
                    ) {
                        Text("Calibrate", fontSize = 11.sp)
                    }

                    if (hasCalibration) {
                        IconButton(onClick = onResetClick, modifier = Modifier.size(32.dp)) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset baseline", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}
