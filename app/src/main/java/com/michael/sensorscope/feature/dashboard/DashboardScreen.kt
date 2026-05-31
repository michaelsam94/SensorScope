package com.michael.sensorscope.feature.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.michael.sensorscope.domain.model.SensorCategory
import com.michael.sensorscope.domain.model.SensorHealth
import com.michael.sensorscope.domain.model.SensorStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onSensorClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Construction,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            "SensorScope",
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Hardware Diagnostic Dashboard",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
            
            Text(
                text = "Real-time 60 Hz system diagnostics sensor evaluation grid.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Category filter chips
            CategoryFilterRow(
                selected = selectedCategory,
                onSelected = { viewModel.selectCategory(it) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            when (val state = uiState) {
                is DashboardUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.testTag("dashboard_loader"))
                    }
                }
                is DashboardUiState.Success -> {
                    if (state.sensors.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.SensorsOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "No sensors found in this category",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    } else {
                        val configuration = LocalConfiguration.current
                        val columns = when {
                            configuration.screenWidthDp >= 840 -> GridCells.Fixed(4)
                            configuration.screenWidthDp >= 600 -> GridCells.Fixed(3)
                            else -> GridCells.Fixed(2)
                        }

                        LazyVerticalGrid(
                            columns = columns,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("sensor_grid")
                        ) {
                            items(state.sensors, key = { it.sensorInfo.type }) { status ->
                                SensorTileCard(
                                    status = status,
                                    onClick = { onSensorClick(status.sensorInfo.type) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryFilterRow(
    selected: SensorCategory?,
    onSelected: (SensorCategory?) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(vertical = 4.dp)
            .testTag("category_filter_row"),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selected == null,
            onClick = { onSelected(null) },
            label = { Text("All", fontFamily = FontFamily.Monospace) }
        )
        
        SensorCategory.values().forEach { category ->
            FilterChip(
                selected = selected == category,
                onClick = { onSelected(category) },
                label = {
                    Text(
                        category.name,
                        fontFamily = FontFamily.Monospace
                    )
                }
            )
        }
    }
}

@Composable
fun SensorTileCard(
    status: SensorStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val info = status.sensorInfo
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("sensor_card_${info.type}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = when (info.category) {
                        SensorCategory.MOTION -> Icons.Default.DirectionsRun
                        SensorCategory.ENVIRONMENT -> Icons.Default.Thermostat
                        SensorCategory.POSITION -> Icons.Default.MyLocation
                        else -> Icons.Default.Sensors
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
                
                SensorStatusBadge(health = status.health)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = info.name,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = info.vendor,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Min delay: ${info.minDelay} µs",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun SensorStatusBadge(health: SensorHealth) {
    val colors = when (health) {
        SensorHealth.OK -> AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            labelColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
        SensorHealth.DEGRADED -> AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            labelColor = MaterialTheme.colorScheme.onErrorContainer
        )
        SensorHealth.FAULT -> AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.error,
            labelColor = MaterialTheme.colorScheme.onError
        )
    }

    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Surface(
            color = colors.containerColor,
            shape = MaterialTheme.shapes.extraSmall
        ) {
            Text(
                text = health.name,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                color = colors.labelColor,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}
