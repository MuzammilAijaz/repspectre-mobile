package com.example.esp32_mpu6050_mobile_data_collection

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.esp32_mpu6050_mobile_data_collection.ui.theme.Esp32mpu6050mobiledatacollectionTheme
import com.example.esp32_mpu6050_mobile_data_collection.BLE.ConnectGATTSample

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Esp32mpu6050mobiledatacollectionTheme {
                ConnectGATTSample()
            }
        }
    }
}