package com.example.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sensor_baselines")
data class SensorBaselineEntity(
    @PrimaryKey val sensorType: Int,
    val sensorName: String,
    val xOffset: Float,
    val yOffset: Float,
    val zOffset: Float,
    val timestamp: Long
)
