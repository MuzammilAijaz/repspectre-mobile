package com.example.esp32_mpu6050_mobile_data_collection.data

import android.bluetooth.BluetoothDevice
import kotlinx.coroutines.flow.MutableStateFlow

interface BleRepository {
    val _device: BluetoothDevice?
    val _connectionState: DeviceConnectionState?
}

class AppBleRepository(
    override val _device: BluetoothDevice? = null,
    override val _connectionState: DeviceConnectionState? = DeviceConnectionState.None,
) : BleRepository {
    public val connectionState = MutableStateFlow(_connectionState)
    public val device = MutableStateFlow(_device)

    public fun getDataFromService() {

    }
}

object appBleRespository {

    val respository = AppBleRepository()
}