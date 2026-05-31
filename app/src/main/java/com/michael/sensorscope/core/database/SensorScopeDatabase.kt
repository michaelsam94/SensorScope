package com.michael.sensorscope.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [SensorBaselineEntity::class, ThermalStressLogEntity::class],
    version = 1,
    exportSchema = false
)
abstract class SensorScopeDatabase : RoomDatabase() {
    abstract fun sensorBaselineDao(): SensorBaselineDao
    abstract fun thermalStressLogDao(): ThermalStressLogDao

    companion object {
        @Volatile
        private var INSTANCE: SensorScopeDatabase? = null

        fun getDatabase(context: Context): SensorScopeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SensorScopeDatabase::class.java,
                    "sensorscope_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
