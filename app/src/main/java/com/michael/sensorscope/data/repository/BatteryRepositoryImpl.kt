package com.michael.sensorscope.data.repository

import com.michael.sensorscope.core.battery.BatteryDataSource
import com.michael.sensorscope.domain.model.BatteryReading
import com.michael.sensorscope.domain.repository.BatteryRepository
import kotlinx.coroutines.flow.Flow

class BatteryRepositoryImpl(private val batteryDataSource: BatteryDataSource) : BatteryRepository {
    override fun observeBatteryTelemetry(): Flow<BatteryReading> {
        return batteryDataSource.observeBattery()
    }
}
