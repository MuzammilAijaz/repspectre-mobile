package com.example.esp32_mpu6050_mobile_data_collection.BLE

import android.Manifest
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import com.example.esp32_mpu6050_mobile_data_collection.service.AppService
import java.math.RoundingMode
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.DecimalFormat

class AppBluetoothGattCallback() : BluetoothGattCallback() {

    // =====================================================================================
    // |                            GATT Connection Callbacks
    // =====================================================================================
    // | This callback will notify us when things change in the GATT connection so we can update
    // | our state
    // |
    // | Functions are called automatically, leading to change in state, leading to recomposition
    // =====================================================================================

    override fun onConnectionStateChange(
        gatt: BluetoothGatt,
        status: Int,
        newState: Int,
    ) {
        super.onConnectionStateChange(gatt, status, newState)
        AppService.bleState.updateConnection(gatt = gatt, connectionState = newState)

        if (status != BluetoothGatt.GATT_SUCCESS) {
            // Here you should handle the error returned in status based on the constants
            // https://developer.android.com/reference/android/bluetooth/BluetoothGatt#summary
            // For example for GATT_INSUFFICIENT_ENCRYPTION or
            // GATT_INSUFFICIENT_AUTHENTICATION you should create a bond.
            // https://developer.android.com/reference/android/bluetooth/BluetoothDevice#createBond()
            Log.e("BLEConnectEffect", "An error happened: $status")
        }
    }

    override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
        super.onMtuChanged(gatt, mtu, status)
        AppService.bleState.updateConnection(gatt = gatt, mtu = mtu)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
        super.onServicesDiscovered(gatt, status)
        AppService.bleState.updateConnection(services = gatt.services)

        // ------------------ Print all Services and Characteristics ------------------
        Log.d("GATT Data", "FOUND, status: ${status}")
        gatt.services.forEach { service ->
            Log.d("GATT Data", "Service: ${service.uuid}")
            service.characteristics.forEach { characteristic ->
                Log.d("GATT Data", "Service: ${characteristic.uuid}")
            }
        }
    }

    override fun onCharacteristicWrite(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?,
        status: Int,
    ) {
        super.onCharacteristicWrite(gatt, characteristic, status)
        AppService.bleState.updateConnection(messageSent = status == BluetoothGatt.GATT_SUCCESS)
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onCharacteristicRead(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        status: Int,
    ) {
        super.onCharacteristicRead(gatt, characteristic, status)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            doOnRead(characteristic.value)
        }
    }

    override fun onCharacteristicRead(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
        status: Int,
    ) {
        super.onCharacteristicRead(gatt, characteristic, value, status)
        doOnRead(value)
    }

    override fun onDescriptorWrite(
        gatt: BluetoothGatt,
        descriptor: BluetoothGattDescriptor,
        status: Int
    ) {
        super.onDescriptorWrite(gatt, descriptor, status)
        Log.d("BLE Data", "Descriptor write status: $status value=${descriptor.value?.contentToString()}")
    }

    // ------------------ Characteristic Change ------------------
    /* Old API (android API <=12) : uses 2 arg method
     * New API (android API >=13) : uses 3 arg method
     */
    override fun onCharacteristicChanged(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?
    ) {
        super.onCharacteristicChanged(gatt, characteristic)
        Log.d("BLE Data", "(2 arg)READ AUTOMATICALLY")
        val value = characteristic?.value
        doOnRead(value ?: byteArrayOf())
    }

    override fun onCharacteristicChanged(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray
    ) {
        super.onCharacteristicChanged(gatt, characteristic, value)
        Log.d("BLE Data", "(3 arg)READ AUTOMATICALLY")
        doOnRead(value)
    }

    // Helpers
    // --------------------------------------------------------------

    fun roundOffDecimal(number: Number): Double? {
        val df = DecimalFormat("#.##")
        df.roundingMode = RoundingMode.CEILING
        return df.format(number).toDouble()
    }

    private fun doOnRead(value: ByteArray) {
        val buffer = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN)
        val accelX = roundOffDecimal(buffer.float)
        val accelY = roundOffDecimal(buffer.float)
        val accelZ = roundOffDecimal(buffer.float)
        Log.d("Sensor Data", "Accel: x=$accelX y=$accelY z=$accelZ")

        val messageReceived = value.decodeToString()
        Log.d("Sensor Data", "raw value: ${value}, Decoded: ${messageReceived}")

        AppService.bleState.updateConnection(messageReceived = "Accel: x=$accelX y=$accelY z=$accelZ")
    }
}