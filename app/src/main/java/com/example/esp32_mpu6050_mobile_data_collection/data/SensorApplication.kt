package com.example.esp32_mpu6050_mobile_data_collection.data

import android.app.Application
import android.content.Context

class SensorApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(context = this)
    }
}