package com.example.esp32_mpu6050_mobile_data_collection.raylib

import android.app.NativeActivity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import com.example.esp32_mpu6050_mobile_data_collection.service.AppService

class RaylibActivity : NativeActivity() {

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Launches the AppService which runs on the foreground and calls the function to update
        // the data on the native side
        val intent: Intent = Intent(this, AppService::class.java)
        startService(intent)
    }

    override fun onDestroy() {
        super.onDestroy()

        // Destroy the service on leave
        val intent: Intent = Intent(this, AppService::class.java)
        stopService(intent)
    }
}