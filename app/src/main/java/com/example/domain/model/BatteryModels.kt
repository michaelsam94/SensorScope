package com.example.domain.model

data class BatteryReading(
    val level: Int,           // 0 to 100
    val health: Int,          // Android status, e.g., BATTERY_HEALTH_GOOD
    val voltage: Int,         // mV
    val currentMicroAmp: Int, // µA
    val temperature: Float,    // °C
    val technology: String,
    val isCharging: Boolean
) {
    val healthString: String get() = when (health) {
        2 -> "Good" // BATTERY_HEALTH_GOOD
        3 -> "Overheat" // BATTERY_HEALTH_OVERHEAT
        4 -> "Dead" // BATTERY_HEALTH_DEAD
        5 -> "Over Voltage" // BATTERY_HEALTH_OVER_VOLTAGE
        6 -> "Unspecified Failure"
        else -> "Unknown"
    }

    val isAnomaly: Boolean get() = (voltage < 3400 && voltage > 0) || (temperature > 40f) || (health == 4)
}
