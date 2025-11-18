package com.example.esp32_mpu6050_mobile_data_collection.data

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattService

// ================== Device State ==================
// Holds connection stats like gatt object, mtu number, Connected/Disconnected etc.
data class DeviceConnectionState(
    val gatt: BluetoothGatt?,
    val connectionState: Int,
    val mtu: Int,
    val services: List<BluetoothGattService> = emptyList(),
    val messageSent: Boolean = false,
    val messageReceived: SensorData = SensorData.Quaternion(0f,0f,0f,0f),
) {
    companion object {
        val None = DeviceConnectionState(null, -1, -1)
    }
}
