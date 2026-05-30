package com.example.data.repository

import com.example.core.battery.BatteryDataSource
import com.example.domain.model.BatteryReading
import com.example.domain.repository.BatteryRepository
import kotlinx.coroutines.flow.Flow

class BatteryRepositoryImpl(private val batteryDataSource: BatteryDataSource) : BatteryRepository {
    override fun observeBatteryTelemetry(): Flow<BatteryReading> {
        return batteryDataSource.observeBattery()
    }
}
