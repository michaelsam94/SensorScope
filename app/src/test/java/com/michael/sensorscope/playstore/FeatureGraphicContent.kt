package com.michael.sensorscope.playstore

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.michael.sensorscope.domain.model.SensorHealth
import com.michael.sensorscope.feature.dashboard.SensorStatusBadge
import com.michael.sensorscope.ui.theme.MyApplicationTheme

@Composable
fun FeatureGraphicContent() {
    MyApplicationTheme(darkTheme = true, dynamicColor = false) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF0E1215), Color(0xFF103C3D), Color(0xFFB68A32)),
                    ),
                )
                .padding(horizontal = 72.dp, vertical = 42.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    "SensorScope",
                    color = Color.White,
                    fontSize = 62.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.sp,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Real-time hardware diagnostics for Android sensors, battery, and thermals.",
                    color = Color(0xFFE7F2EF),
                    fontSize = 25.sp,
                    lineHeight = 32.sp,
                    modifier = Modifier.fillMaxWidth(0.86f),
                )
                Spacer(Modifier.height(28.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    FeaturePill(icon = { Icon(Icons.Default.Sensors, null) }, label = "Sensors")
                    FeaturePill(icon = { Icon(Icons.Default.BatteryFull, null) }, label = "Battery")
                    FeaturePill(icon = { Icon(Icons.Default.Thermostat, null) }, label = "Thermals")
                }
            }

            Box(
                modifier = Modifier
                    .width(248.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0xFF11181B))
                    .padding(14.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFF1F5F7))
                        .padding(16.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Speed, null, tint = Color(0xFF0E6364), modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Live Grid", fontWeight = FontWeight.Bold, color = Color(0xFF0E1215))
                    }
                    Spacer(Modifier.height(16.dp))
                    repeat(4) { index ->
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(8.dp),
                            shadowElevation = 1.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column {
                                    Text(
                                        listOf("Accelerometer", "Gyroscope", "Ambient Light", "Barometer")[index],
                                        color = Color(0xFF0E1215),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                    )
                                    Text("60 Hz diagnostic", color = Color(0xFF607078), fontSize = 10.sp)
                                }
                                SensorStatusBadge(if (index == 2) SensorHealth.DEGRADED else SensorHealth.OK)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeaturePill(icon: @Composable () -> Unit, label: String) {
    Surface(
        color = Color.White.copy(alpha = 0.14f),
        contentColor = Color.White,
        shape = RoundedCornerShape(999.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(18.dp), contentAlignment = Alignment.Center) {
                icon()
            }
            Spacer(Modifier.width(8.dp))
            Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}
