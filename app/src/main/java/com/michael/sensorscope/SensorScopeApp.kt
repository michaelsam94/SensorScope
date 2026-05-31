package com.michael.sensorscope

import android.app.Application

class SensorScopeApp : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
