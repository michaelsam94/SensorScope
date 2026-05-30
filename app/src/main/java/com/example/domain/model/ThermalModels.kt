package com.example.domain.model

data class ThermalReading(
    val timestamp: Long,
    val batteryTemp: Float,
    val cpuTemp: Float
)

data class ThermalStressLog(
    val id: Int = 0,
    val timestamp: Long,
    val maxCpuTemp: Float,
    val maxBatteryTemp: Float,
    val durationMs: Long,
    val safetyStopTriggered: Boolean,
    val throttlingDetected: Boolean,
    val healthAssessment: String
)
