package com.example.esp32_mpu6050_mobile_data_collection.BLE

import android.Manifest
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import com.example.esp32_mpu6050_mobile_data_collection.data.SensorData
import com.example.esp32_mpu6050_mobile_data_collection.service.AppService
import com.example.esp32_mpu6050_mobile_data_collection.service.CHARACTERISTIC_UUID
import com.example.esp32_mpu6050_mobile_data_collection.service.SERVICE_UUID
import java.math.RoundingMode
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.DecimalFormat
import java.util.UUID

class AppBluetoothGattCallback(
    private val service: AppService
) : BluetoothGattCallback() {

    val INITIAL_MTU: Int = 500
    // =====================================================================================
    // |                            GATT Connection Callbacks
    // =====================================================================================
    // | This callback will notify us when things change in the GATT connection so we can update
    // | our state
    // |
    // | Functions are called automatically, leading to change in state, leading to recomposition
    // =====================================================================================

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onConnectionStateChange(
        gatt: BluetoothGatt,
        status: Int,
        newState: Int,
    ) {
        super.onConnectionStateChange(gatt, status, newState)
        Log.e("BluetoothCallback", "onConnectionCalled")

        if (newState == BluetoothProfile.STATE_CONNECTED) {
            Log.d("BluetoothCallback", "Connected -> discovering services and requesting MTU")
            // discover services
            gatt.discoverServices()
        }

        else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            // Actually disconnect from device
            gatt.close()
            service.resetBleState() // reset our app state
            Log.d("AppServiceDisconnection", "Gatt Closed")

            // Only called app is closing
            if (service.isServiceDestructionRequired) {
                Log.d("AppServiceDisconnection", "Calling Stop Service")
                service.stopService()
            }
        }

        service.updateConnection(gatt = gatt, connectionState = newState)

        if (status != BluetoothGatt.GATT_SUCCESS) {
            // Here you should handle the error returned in status based on the constants
            // https://developer.android.com/reference/android/bluetooth/BluetoothGatt#summary
            // For example for GATT_INSUFFICIENT_ENCRYPTION or
            // GATT_INSUFFICIENT_AUTHENTICATION you should create a bond.
            // https://developer.android.com/reference/android/bluetooth/BluetoothDevice#createBond()
            Log.e("BluetoothCallback", "An error happened: $status")
        }
    }

    override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
        super.onMtuChanged(gatt, mtu, status)
        service.updateConnection(gatt = gatt, mtu = mtu)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
        super.onServicesDiscovered(gatt, status)

        // Request for MTU
        if (status == BluetoothGatt.GATT_SUCCESS) {
            Log.d("BluetoothCallback", "Services discovered, requesting MTU")
            gatt.requestMtu(INITIAL_MTU)
        }

        service.updateConnection(services = gatt.services)

        // --------- Print all Services and Characteristics --------------
        Log.d("BluetoothCallback", "FOUND, status: ${status}")
        gatt.services.forEach { service ->
            Log.d("BluetoothCallback", "Service: ${service.uuid}")
            service.characteristics.forEach { characteristic ->
                Log.d("BluetoothCallback", "Service: ${characteristic.uuid}")
            }
        }

        // ----- Enable Notifications -----------------------------------
        val gattService = gatt.getService(SERVICE_UUID)
        val characteristic = gattService?.getCharacteristic(CHARACTERISTIC_UUID)

        /** This tells android to look for data instead of sending request<->responses
         *  without Calls the onCharacteristicChanged() on successful write */
        Log.d("BluetoothCallback", "setting notifications")
        gatt.setCharacteristicNotification(characteristic, true)

        // Write to the CCCD of the descriptor of the characteristic we want to read
        // to notify the device to send notifications.
        // The CCCDs values is usually 0x2902
        val descriptor = characteristic?.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
        descriptor?.let {
            val enableNotificationByte = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            it.value = enableNotificationByte // OLD API
            // gatt.writeDescriptor(it, enableNotificationByte) // NEW API 33....
            gatt.writeDescriptor(it) // OLD API
            Log.d("BluetoothCallback", "Written to Descriptor: $enableNotificationByte.toString()")
        }
        // --------------------------------------------------------------

        service.subscribeToService()
    }

    override fun onCharacteristicWrite(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?,
        status: Int,
    ) {
        super.onCharacteristicWrite(gatt, characteristic, status)
        service.updateConnection(messageSent = status == BluetoothGatt.GATT_SUCCESS)
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

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onDescriptorWrite(
        gatt: BluetoothGatt,
        descriptor: BluetoothGattDescriptor,
        status: Int
    ) {
        super.onDescriptorWrite(gatt, descriptor, status)
        Log.d("BluetoothCallback", "Descriptor write status: $status value=${descriptor.value?.contentToString()}")

        val cccdUuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        // Check if the value is for disabling Notifications ; otherwise it will close GATT connection even
        // on enable of notification
        if (descriptor.uuid == cccdUuid && status == BluetoothGatt.GATT_SUCCESS) {
            val value = descriptor.value
            if (value != null && value.contentEquals(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)) {
                Log.d("AppServiceDisconnection", "CCCD disable acknowledged — disconnecting now")
                gatt.disconnect()
            } else {
                Log.d("BluetoothCallback", "CCCD written but not disable — continuing normally")
            }
        }
    }

    // ------------------ Characteristic Change ------------------
    // Handles the Notifications
    /* Old API (android API <=12) : uses 2 arg method
     * New API (android API >=13) : uses 3 arg method
     */
    override fun onCharacteristicChanged(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?
    ) {
        super.onCharacteristicChanged(gatt, characteristic)
        Log.d("BluetoothCallback", "Notification")
        val value = characteristic?.value
        doOnRead(value ?: byteArrayOf())
    }

    override fun onCharacteristicChanged(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray
    ) {
        super.onCharacteristicChanged(gatt, characteristic, value)
        Log.d("BluetoothCallback", "Notification")
        doOnRead(value)
    }

    // Helpers
    // --------------------------------------------------------------

    fun roundOffDecimal(number: Number): Double? {
        val df = DecimalFormat("#.####")
        df.roundingMode = RoundingMode.CEILING
        return df.format(number).toDouble()
    }

    private fun doOnRead(value: ByteArray) {
        Log.d("BluetoothCallbackNotification", "Notification size = ${value.size}")

// ----- QUATERNIONS ----------------------------------------
        val buffer = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN)
        val x = roundOffDecimal(buffer.float)?.toFloat()?:0f
        val y = roundOffDecimal(buffer.float)?.toFloat()?:0f
        val z = roundOffDecimal(buffer.float)?.toFloat()?:0f
        val w = roundOffDecimal(buffer.float)?.toFloat()?:0f
        Log.d("BluetoothCallbackValues", "Accel: x=$x y=$y z=$z, z=$w")

        service.updateConnection(messageReceived = SensorData.Quaternion(x,y,z,w))

// ----- Raw Data Values ------------------------------------
//        val buffer = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN)
//        val ax = roundOffDecimal(buffer.float)?.toFloat()?:0f
//        val ay = roundOffDecimal(buffer.float)?.toFloat()?:0f
//        val az = roundOffDecimal(buffer.float)?.toFloat()?:0f
//
//        val gx = roundOffDecimal(buffer.float)?.toFloat()?:0f
//        val gy = roundOffDecimal(buffer.float)?.toFloat()?:0f
//        val gz = roundOffDecimal(buffer.float)?.toFloat()?:0f
//        Log.d("BluetoothCallbackValues", "Accel: ax=$ax ay=$ay az=$az, gx=$gx, gy=$gy, gz=$gz")
//
//        service.updateConnection(messageReceived = SensorData.Raw(ax,ay,az,gx,gy,gz))
    }
}

/* =====================================================================================
 * |                                  WHAT I WAS DOING
 * | -----------------------------------------------------------------------------------
 * | - some kinda underflow is happening,
 * | - im not getting values and doOnRead is not being called,
 * | - i was adding rawData as a sensorData value, but i couldnt see the data getting inserted
 * |    into the database or even no printing.
 * |
 * ===================================================================================== */
