package com.example.core.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import com.example.domain.model.SensorCategory
import com.example.domain.model.SensorInfo
import com.example.domain.model.SensorReading
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

interface SensorDataSource {
    fun getSystemSensors(): List<SensorInfo>
    fun observeSensor(sensorType: Int): Flow<SensorReading>
}

class SensorDataSourceImpl(private val context: Context) : SensorDataSource {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    
    private val sensorThread = HandlerThread("SensorThread").apply { start() }
    private val sensorHandler = Handler(sensorThread.looper)
    
    override fun getSystemSensors(): List<SensorInfo> {
        val sensors = sensorManager.getSensorList(Sensor.TYPE_ALL)
        return sensors.map { mapAndroidSensorToDomain(it) }
    }
    
    override fun observeSensor(sensorType: Int): Flow<SensorReading> = callbackFlow {
        val sensor = sensorManager.getDefaultSensor(sensorType)
        if (sensor == null) {
            close()
            return@callbackFlow
        }
        
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event != null && event.sensor.type == sensorType) {
                    val reading = SensorReading(
                        timestamp = System.currentTimeMillis(),
                        values = event.values.toList()
                    )
                    trySend(reading)
                }
            }
            
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        
        sensorManager.registerListener(
            listener,
            sensor,
            SensorManager.SENSOR_DELAY_UI, // SENSOR_DELAY_UI matches typical 60Hz displays and prevents buffering overflow
            sensorHandler
        )
        
        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
}

fun mapAndroidSensorToDomain(s: Sensor): SensorInfo {
    val category = when (s.type) {
        Sensor.TYPE_ACCELEROMETER,
        Sensor.TYPE_GYROSCOPE,
        Sensor.TYPE_GRAVITY,
        Sensor.TYPE_LINEAR_ACCELERATION,
        Sensor.TYPE_ROTATION_VECTOR -> SensorCategory.MOTION
        
        Sensor.TYPE_LIGHT,
        Sensor.TYPE_AMBIENT_TEMPERATURE,
        Sensor.TYPE_PRESSURE,
        Sensor.TYPE_RELATIVE_HUMIDITY -> SensorCategory.ENVIRONMENT
        
        Sensor.TYPE_MAGNETIC_FIELD,
        Sensor.TYPE_ORIENTATION,
        Sensor.TYPE_PROXIMITY -> SensorCategory.POSITION
        
        else -> SensorCategory.UNKNOWN
    }
    
    return SensorInfo(
        type = s.type,
        name = s.name,
        vendor = s.vendor,
        version = s.version,
        maximumRange = s.maximumRange,
        resolution = s.resolution,
        power = s.power,
        minDelay = s.minDelay,
        category = category
    )
}
