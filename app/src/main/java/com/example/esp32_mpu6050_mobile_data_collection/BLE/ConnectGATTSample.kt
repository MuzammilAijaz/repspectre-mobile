/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.esp32_mpu6050_mobile_data_collection.BLE

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothProfile
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.math.RoundingMode
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.DecimalFormat
import java.util.UUID
import kotlin.random.Random

// =====================================================================================
// |                                   Constants
// =====================================================================================

// UUID for our service known between the client and server to allow communication
private val SERVICE_UUID: UUID = UUID.fromString("efcdab90-7856-3412-f0de-bc9a78563412")
// Same as the service but for the characteristic
private val CHARACTERISTIC_UUID: UUID = UUID.fromString("badcfe10-3254-7698-badc-fe1032547698")

// =====================================================================================
// |                                 Functions
// =====================================================================================

/**
 * Writes "hello world" to the server characteristic
 */
@SuppressLint("MissingPermission")
fun sendData(
    gatt: BluetoothGatt,
    characteristic: BluetoothGattCharacteristic,
) {
    val data = "Hello world!".toByteArray()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        gatt.writeCharacteristic(
            characteristic,
            data,
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT,
        )
    } else {
        characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        @Suppress("DEPRECATION")
        characteristic.value = data
        @Suppress("DEPRECATION")
        gatt.writeCharacteristic(characteristic)
    }
}

internal fun Int.toConnectionStateString() = when (this) {
    BluetoothProfile.STATE_CONNECTED -> "Connected"
    BluetoothProfile.STATE_CONNECTING -> "Connecting"
    BluetoothProfile.STATE_DISCONNECTED -> "Disconnected"
    BluetoothProfile.STATE_DISCONNECTING -> "Disconnecting"
    else -> "N/A"
}

//// ================== Bluetooth Logic ==================
//@SuppressLint("InlinedApi")
//@RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
//@Composable
//private fun BLEConnectEffect(
//    device: BluetoothDevice,
//    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
//    indications: Boolean,
//    onStateChange: (DeviceConnectionState) -> Unit,
//) {
//    val context = LocalContext.current
//    val currentOnStateChange by rememberUpdatedState(onStateChange)
//
//    // Keep the current connection state
//    var state by remember {
//        mutableStateOf(DeviceConnectionState.None)
//    }
//
//    // Disposable -> GATT connection is tied to this composables lifetime, on exit, calls dispose function which calls the gatt.close()
//    DisposableEffect(lifecycleOwner, device) {
//
//        // =====================================================================================
//        // |                            GATT Connection Callbacks
//        // =====================================================================================
//        // | This callback will notify us when things change in the GATT connection so we can update
//        // | our state
//        // |
//        // | Functions are called automatically, leading to change in state, leading to recomposition
//        // =====================================================================================
//        val callback = object : BluetoothGattCallback() {
//
//            override fun onConnectionStateChange(
//                gatt: BluetoothGatt,
//                status: Int,
//                newState: Int,
//            ) {
//                super.onConnectionStateChange(gatt, status, newState)
//                state = state.copy(gatt = gatt, connectionState = newState)
//                currentOnStateChange(state)
//
//                if (status != BluetoothGatt.GATT_SUCCESS) {
//                    // Here you should handle the error returned in status based on the constants
//                    // https://developer.android.com/reference/android/bluetooth/BluetoothGatt#summary
//                    // For example for GATT_INSUFFICIENT_ENCRYPTION or
//                    // GATT_INSUFFICIENT_AUTHENTICATION you should create a bond.
//                    // https://developer.android.com/reference/android/bluetooth/BluetoothDevice#createBond()
//                    Log.e("BLEConnectEffect", "An error happened: $status")
//                }
//            }
//
//            override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
//                super.onMtuChanged(gatt, mtu, status)
//                state = state.copy(gatt = gatt, mtu = mtu)
//                currentOnStateChange(state)
//            }
//
//            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
//            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
//                super.onServicesDiscovered(gatt, status)
//                state = state.copy(services = gatt.services)
//
//                // ------------------ Print all Services and Characteristics ------------------
//                Log.d("GATT Data", "FOUND, status: ${status}")
//                gatt.services.forEach { service ->
//                    Log.d("GATT Data", "Service: ${service.uuid}")
//                    service.characteristics.forEach { characteristic ->
//                        Log.d("GATT Data", "Service: ${characteristic.uuid}")
//                    }
//                }
//
//                currentOnStateChange(state)
//            }
//
//            override fun onCharacteristicWrite(
//                gatt: BluetoothGatt?,
//                characteristic: BluetoothGattCharacteristic?,
//                status: Int,
//            ) {
//                super.onCharacteristicWrite(gatt, characteristic, status)
//                state = state.copy(messageSent = status == BluetoothGatt.GATT_SUCCESS)
//                currentOnStateChange(state)
//            }
//
//            @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
//            override fun onCharacteristicRead(
//                gatt: BluetoothGatt,
//                characteristic: BluetoothGattCharacteristic,
//                status: Int,
//            ) {
//                super.onCharacteristicRead(gatt, characteristic, status)
//                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
//                    doOnRead(characteristic.value)
//                }
//            }
//
//            override fun onCharacteristicRead(
//                gatt: BluetoothGatt,
//                characteristic: BluetoothGattCharacteristic,
//                value: ByteArray,
//                status: Int,
//            ) {
//                super.onCharacteristicRead(gatt, characteristic, value, status)
//                doOnRead(value)
//            }
//
//            override fun onDescriptorWrite(
//                gatt: BluetoothGatt,
//                descriptor: BluetoothGattDescriptor,
//                status: Int
//            ) {
//                super.onDescriptorWrite(gatt, descriptor, status)
//                Log.d("BLE Data", "Descriptor write status: $status value=${descriptor.value?.contentToString()}")
//            }
//
//            // ------------------ Characteristic Change ------------------
//            /* Old API (android API <=12) : uses 2 arg method
//             * New API (android API >=13) : uses 3 arg method
//             */
//            override fun onCharacteristicChanged(
//                gatt: BluetoothGatt?,
//                characteristic: BluetoothGattCharacteristic?
//            ) {
//                super.onCharacteristicChanged(gatt, characteristic)
//                Log.d("BLE Data", "(2 arg)READ AUTOMATICALLY")
//                val value = characteristic?.value
//                doOnRead(value ?: byteArrayOf())
//            }
//
//            override fun onCharacteristicChanged(
//                gatt: BluetoothGatt,
//                characteristic: BluetoothGattCharacteristic,
//                value: ByteArray
//            ) {
//                super.onCharacteristicChanged(gatt, characteristic, value)
//                Log.d("BLE Data", "(3 arg)READ AUTOMATICALLY")
//                doOnRead(value)
//            }
//            // ---------------------------------------------
//
//            fun roundOffDecimal(number: Number): Double? {
//                val df = DecimalFormat("#.##")
//                df.roundingMode = RoundingMode.CEILING
//                return df.format(number).toDouble()
//            }
//
//            private fun doOnRead(value: ByteArray) {
//                val buffer = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN)
//                val accelX = roundOffDecimal(buffer.float)
//                val accelY = roundOffDecimal(buffer.float)
//                val accelZ = roundOffDecimal(buffer.float)
//                Log.d("Sensor Data", "Accel: x=$accelX y=$accelY z=$accelZ")
//
//                val messageReceived = value.decodeToString()
//                Log.d("Sensor Data", "raw value: ${value}, Decoded: ${messageReceived}")
//                state = state.copy(messageReceived = "Accel: x=$accelX y=$accelY z=$accelZ")
//                currentOnStateChange(state)
//            }
//        }
//
//        val observer = LifecycleEventObserver { _, event ->
//            if (event == Lifecycle.Event.ON_START) {
//                if (state.gatt != null) {
//                    // If we previously had a GATT connection let's reestablish it
//                    state.gatt?.connect()
//                } else {
//                    // Otherwise create a new GATT connection
//                    state = state.copy(gatt = device.connectGatt(context, false, callback))
//                }
//            } else if (event == Lifecycle.Event.ON_STOP) {
//                // Unless you have a reason to keep connected while in the bg you should disconnect
//                state.gatt?.connect()
//            }
//        }
//
//        // Add the observer to the lifecycle
//        lifecycleOwner.lifecycle.addObserver(observer)
//
//        // When the effect leaves the Composition, remove the observer and close the connection
//        onDispose {
//            lifecycleOwner.lifecycle.removeObserver(observer)
//            state.gatt?.close()
//            state = DeviceConnectionState.None
//        }
//    }
//}