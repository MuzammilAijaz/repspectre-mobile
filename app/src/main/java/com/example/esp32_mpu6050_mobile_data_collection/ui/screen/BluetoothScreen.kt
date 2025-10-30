package com.example.esp32_mpu6050_mobile_data_collection.ui.screen

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.esp32_mpu6050_mobile_data_collection.BLE.FindDevicesScreen
import java.util.jar.Manifest

// =====================================================================================
// |                                Main Composable
// =====================================================================================

@SuppressLint("MissingPermission")
@OptIn(ExperimentalAnimationApi::class)
@RequiresApi(Build.VERSION_CODES.M)
@Composable
fun ConnectGATTSample() {
    var selectedDevice by remember {
        mutableStateOf<BluetoothDevice?>(null)
    }
    // Check that BT permissions and that BT is available and enabled
    _root_ide_package_.com.example.platform.connectivity.bluetooth.ble.BluetoothSampleBox {
        AnimatedContent(targetState = selectedDevice, label = "Selected device") { device ->
            if (device == null) {
                // Scans for BT devices and handles clicks (see FindDeviceSample)
                FindDevicesScreen {
                    selectedDevice = it
                }
            } else {
                // Once a device is selected show the UI and try to connect device
                ConnectDeviceScreen(device = device as BluetoothDevice) {
                    selectedDevice = null
                }
            }
        }
    }
}

@SuppressLint("InlinedApi")
@RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
@Composable
fun ConnectDeviceScreen(device: BluetoothDevice, onClose: () -> Unit) {
    val scope = rememberCoroutineScope()

    // Keeps track of the last connection state with the device
    var state by remember(device) {
        mutableStateOf<DeviceConnectionState?>(null)
    }
    // Once the device services are discovered find the GATTServerSample service
    val service by remember(state?.services) {
        mutableStateOf(state?.services?.find { it.uuid == SERVICE_UUID })
    }
    // If the GATTServerSample service is found, get the characteristic
    val characteristic by remember(service) {
        mutableStateOf(service?.getCharacteristic(CHARACTERISTIC_UUID))
    }
    var indications by remember { mutableStateOf(false) }

    // This effect will handle the connection and notify when the state changes
    BLEConnectEffect(device = device, indications = indications) {
        // update our state to recompose the UI
        state = it
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = "Devices details", style = MaterialTheme.typography.headlineSmall)
        Text(text = "Name: ${device.name} (${device.address})")
        Text(text = "Status: ${state?.connectionState?.toConnectionStateString()}")
        Text(text = "MTU: ${state?.mtu}")
        Text(text = "Services: ${state?.services?.joinToString { it.uuid.toString() + " " + it.type }}")
        Text(text = "Message sent: ${state?.messageSent}")

        val service = state?.services?.find { it.uuid == SERVICE_UUID }
        val characteristic = service?.getCharacteristic(CHARACTERISTIC_UUID)
        Log.d("Sensor Data", "Message Value: ${state?.messageReceived}")
        Log.d("Sensor Data", "Characteristic Value: ${characteristic}")
        Log.d("Sensor Data", "Service Value: ${service}")

        Text(text = "Message received: ${state?.messageReceived}")
        Button(
            onClick = {
                scope.launch(Dispatchers.IO) {
                    if (state?.connectionState == BluetoothProfile.STATE_DISCONNECTED) {
                        state?.gatt?.connect()
                    }
                    // Example on how to request specific MTUs
                    // Note that from Android 14 onwards the system will define a default MTU and
                    // it will only be sent once to the peripheral device
                    state?.gatt?.requestMtu(Random.nextInt(27, 190))
                }
            },
        ) {
            Text(text = "Request MTU")
        }
        Button(
            enabled = state?.gatt != null,
            onClick = {
                scope.launch(Dispatchers.IO) {
                    // Once we have the connection discover the peripheral services
                    state?.gatt?.discoverServices()
                }
            },
        ) {
            Text(text = "Discover")
        }
        Button(
            enabled = state?.gatt != null && characteristic != null,
            onClick = {
                scope.launch(Dispatchers.IO) {
                    sendData(state?.gatt!!, characteristic!!)
                }
            },
        ) {
            Text(text = "Write to server")
        }
        Button(
            enabled = state?.gatt != null && characteristic != null,
            onClick = {
                scope.launch(Dispatchers.IO) {
                    state?.gatt?.readCharacteristic(characteristic)
                }
            },
        ) {
            Text(text = "Read characteristic")
        }

        Button(
            onClick = {
                indications = !indications
                Log.d("BLE Data", "Indications: $indications")

                val characteristic = state?.gatt
                    ?.getService(SERVICE_UUID)
                    ?.getCharacteristic(CHARACTERISTIC_UUID)

                if (characteristic != null) {
                    state?.gatt?.setCharacteristicNotification(characteristic, indications)

                    val descriptor = characteristic.getDescriptor(
                        UUID.fromString("00002902-0000-1000-8000-00805F9B34FB")
                    )
                    descriptor?.value = if (indications)
                        BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
                    else
                        BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE

                    Log.d("BLE Data", "${descriptor?.value}")

                    state?.gatt?.writeDescriptor(descriptor)
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (indications) Color.Green else Color.Gray
            )
        ) {
            Text(if (indications) "INDICATIONS ON" else "INDICATIONS OFF")
        }

        Button(onClick = onClose) {
            Text(text = "Close")
        }
    }
}
