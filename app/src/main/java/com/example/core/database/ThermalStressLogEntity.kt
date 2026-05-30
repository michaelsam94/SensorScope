package com.example.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "thermal_stress_logs")
data class ThermalStressLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val maxCpuTemp: Float,
    val maxBatteryTemp: Float,
    val durationMs: Long,
    val safetyStopTriggered: Boolean
)
