package com.example.esp32_mpu6050_mobile_data_collection.data

import android.util.Log
import com.example.esp32_mpu6050_mobile_data_collection.service.AppService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

interface BleRepository {
    var message: String?
}

class AppBleRepository(
) : BleRepository {

    override var message: String? = null

    val messages = AppService.messages

    init {
        CoroutineScope(Dispatchers.IO).launch {
            messages.collect { msg ->
                if (msg != null) {
                    message = msg
                    Log.d("BLE Data", "Got Data")
                }
            }
        }
    }

}

object appBleRespositoryProvider {
    val respository = AppBleRepository()
}