package com.michael.sensorscope.domain.model

enum class SensorCategory {
    MOTION,
    ENVIRONMENT,
    POSITION,
    UNKNOWN
}

enum class SensorHealth {
    OK,
    DEGRADED,
    FAULT
}

data class SensorInfo(
    val type: Int,
    val name: String,
    val vendor: String,
    val version: Int,
    val maximumRange: Float,
    val resolution: Float,
    val power: Float,
    val minDelay: Int,
    val category: SensorCategory
)

data class SensorReading(
    val timestamp: Long,
    val values: List<Float>
) {
    val x: Float get() = values.getOrNull(0) ?: 0f
    val y: Float get() = values.getOrNull(1) ?: 0f
    val z: Float get() = values.getOrNull(2) ?: 0f
}

data class SensorStatus(
    val sensorInfo: SensorInfo,
    val isAvailable: Boolean = true,
    val health: SensorHealth = SensorHealth.OK,
    val baselineX: Float = 0f,
    val baselineY: Float = 0f,
    val baselineZ: Float = 0f
)

data class SensorBaseline(
    val sensorType: Int,
    val sensorName: String,
    val xOffset: Float,
    val yOffset: Float,
    val zOffset: Float,
    val timestamp: Long
)
