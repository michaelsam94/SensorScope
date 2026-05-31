package com.michael.sensorscope.core.battery

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.michael.sensorscope.domain.model.BatteryReading
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

interface BatteryDataSource {
    fun observeBattery(): Flow<BatteryReading>
    fun getLatestBatteryReading(): BatteryReading
}

class BatteryDataSourceImpl(private val context: Context) : BatteryDataSource {
    private val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

    override fun observeBattery(): Flow<BatteryReading> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent != null && intent.action == Intent.ACTION_BATTERY_CHANGED) {
                    trySend(mapIntentToReading(intent))
                }
            }
        }
        
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        
        awaitClose {
            context.unregisterReceiver(receiver)
        }
    }
    
    override fun getLatestBatteryReading(): BatteryReading {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        return if (intent != null) {
            mapIntentToReading(intent)
        } else {
            BatteryReading(
                level = 100,
                health = 2, // GOOD
                voltage = 3800,
                currentMicroAmp = 0,
                temperature = 25f,
                technology = "Li-ion",
                isCharging = false
            )
        }
    }

    private fun mapIntentToReading(intent: Intent): BatteryReading {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val pct = if (level >= 0 && scale > 0) (level * 100 / scale) else -1
        
        val health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, 1) // BATTERY_HEALTH_UNKNOWN = 1
        val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) // mV
        
        val currentVal = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        
        val temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f // convert tenths of a degree to Celsius
        val tech = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Li-ion"
        
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == 2 || status == 5 // CHARGING = 2, FULL = 5

        return BatteryReading(
            level = pct,
            health = health,
            voltage = voltage,
            currentMicroAmp = currentVal,
            temperature = temp,
            technology = tech,
            isCharging = isCharging
        )
    }
}
