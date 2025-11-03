package com.example.esp32_mpu6050_mobile_data_collection.ui.screen

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.esp32_mpu6050_mobile_data_collection.BLE.BluetoothSampleBox
import com.example.esp32_mpu6050_mobile_data_collection.BLE.FindDevicesScreen
import com.example.esp32_mpu6050_mobile_data_collection.BLE.sendData
import com.example.esp32_mpu6050_mobile_data_collection.BLE.toConnectionStateString
import com.example.esp32_mpu6050_mobile_data_collection.raylib.RaylibActivity
import com.example.esp32_mpu6050_mobile_data_collection.service.AppService
import com.example.esp32_mpu6050_mobile_data_collection.service.AppService.bleStateProvider.state
import com.example.esp32_mpu6050_mobile_data_collection.ui.view.AppViewModel
import com.example.esp32_mpu6050_mobile_data_collection.ui.view.BleViewModel
import com.example.platform.connectivity.bluetooth.ble.server.GATTServerSampleService.Companion.CHARACTERISTIC_UUID
import com.example.platform.connectivity.bluetooth.ble.server.GATTServerSampleService.Companion.SERVICE_UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.random.Random

// =====================================================================================
// |                                Main Composable
// =====================================================================================

@SuppressLint("MissingPermission")
@OptIn(ExperimentalAnimationApi::class)
@RequiresApi(Build.VERSION_CODES.M)
@Composable
fun ConnectGATTSample() {
    val viewModel: BleViewModel = viewModel(factory = BleViewModel.Factory)

    var selectedDevice by remember {
        mutableStateOf<BluetoothDevice?>(null)
    }
    // Check that BT permissions and that BT is available and enabled
    BluetoothSampleBox {
        AnimatedContent(targetState = selectedDevice, label = "Selected device") { device ->
            if (device == null) {
                // Scans for BT devices and handles clicks (see FindDeviceSample)
                FindDevicesScreen {
                    selectedDevice = it
                }
            } else {
                // Once a device is selected show the UI and try to connect device
                ConnectDeviceScreen(device = device as BluetoothDevice, viewModel) {
                    selectedDevice = null
                }
            }
        }
    }
}

// =====================================================================================
// |                                     UI
// =====================================================================================
// | NOTES AND CONSIDERATIONS:
// |    -> right now implemntation requires duplicate of code in :
// |        -> stopping the saving of data to the database
// |
// =====================================================================================
@SuppressLint("InlinedApi")
@RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
@Composable
fun ConnectDeviceScreen(device: BluetoothDevice, viewModel: BleViewModel, onClose: () -> Unit) {
    val appViewModel: AppViewModel = viewModel(factory = AppViewModel.Factory)
    var isStoreDataOn: Boolean by remember { mutableStateOf(false) }
    var isNewSession: Boolean by remember { mutableStateOf(false) }

//    val scope = rememberCoroutineScope()
//
//    // Keeps track of the last connection state with the device
//    val state = viewModel.uiState.collectAsState().value.connectionState

//    // Once the device services are discovered find the GATTServerSample service
//    val service by remember(state?.services) {
//        mutableStateOf(state?.services?.find { it.uuid == SERVICE_UUID })
//    }
//    // If the GATTServerSample service is found, get the characteristic
//    val characteristic by remember(service) {
//        mutableStateOf(service?.getCharacteristic(CHARACTERISTIC_UUID))
//    }
//    var indications by remember { mutableStateOf(false) }

    // This service will handle the connection and update the BleRepository
    val context = LocalContext.current
    val intent = Intent(context, AppService::class.java).apply {
        putExtra("BLE_DEVICE", device) // pass in extra arguments inside the intent
    }
    context.startService(intent)

//    BLEConnectEffect(device = device, indications = indications, viewModel = viewModel)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = "Devices details", style = MaterialTheme.typography.headlineSmall)
        Text(text = "Name: ${device.name} (${device.address})")

//        Text(text = "Status: ${state?.connectionState?.toConnectionStateString()}")
        Text(text = "Status: ${viewModel.getConnectionState()}")

        Text(text = "MTU: ${state?.mtu}")
        Text(text = "Services: ${state?.services?.joinToString { it.uuid.toString() + " " + it.type }}")
        Text(text = "Message sent: ${state?.messageSent}")

        val service = state?.services?.find { it.uuid == SERVICE_UUID }
        val characteristic = service?.getCharacteristic(CHARACTERISTIC_UUID)
        Log.d("Sensor Data", "Message Value: ${state?.messageReceived}")
        Log.d("Sensor Data", "Characteristic Value: ${characteristic}")
        Log.d("Sensor Data", "Service Value: ${service}")

        Text(text = "Message received: ${state?.messageReceived}")

        if (isStoreDataOn) {
            if(isNewSession) {appViewModel.createNewSession() ; isNewSession = false}

            Log.d("Database", "Collection Started")
            appViewModel.insertValue(state?.messageReceived ?: "Accel: x=0.0 y=0.0 z=0.0")
        }

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

        Text(
            text="Store is ${if (isStoreDataOn) "enabled" else "disabled"}",
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        var isTimerModeOn: Boolean by remember { mutableStateOf(false) }
        Row(

        ) {
            Button(
                onClick = {
                    isTimerModeOn = !isTimerModeOn
                },
                enabled = !isTimerModeOn,
            ) {
                Text(text = "Enable Timer Mode (10 seconds)")
            }

            Button(
                onClick = {
                    isTimerModeOn = !isTimerModeOn
                },
                enabled = isTimerModeOn,
            ) {
                Text(text = "Stop Timer Mode")
            }
        }

        if (isStoreDataOn) {
            val currentTime = System.currentTimeMillis()
            val startTime by remember { mutableLongStateOf(currentTime) }
            val duration = currentTime - startTime
            Text(text = "Time: ${(duration) / 1000}")

            if (isTimerModeOn) {
                if ( duration > 10000 ) { // if greater than 10 seconds, close database connection
                    isStoreDataOn = false
                    isNewSession = false
                    appViewModel.stopOldSession()
                }
            }
        }
        Button(
            onClick = {
                if(isStoreDataOn == false) {
                    isStoreDataOn = true
                    isNewSession = true
                    // TODO() : serapte the UI and ViewModel Logic so i can do appViewModel.createNewSession() instead
                }
            },
            enabled = !isStoreDataOn,
            modifier = Modifier.background(if (!isStoreDataOn) Color.Red else Color.Green ).align(Alignment.CenterHorizontally)
        ) {
            Text(text = "Start")
        }

        Button(
            onClick = {
                if(isStoreDataOn == true) {
                    isStoreDataOn = false
                    isNewSession = false
                    appViewModel.stopOldSession()
                }
            },
            enabled = isStoreDataOn,
            modifier = Modifier.background(if (isStoreDataOn) Color.Red else Color.Green ).align(Alignment.CenterHorizontally)
        ) {
            Text(text = "Stop")
        }

        Button(onClick = {
            // Close GATT connection
            viewModel.disconnectAndClose()
            // Open device selection screen
            onClose()
            }
        ) {
            Text(text = "Close")
        }

        Button(onClick = {
            appViewModel.cleanDatabase()
            },
            enabled = !isStoreDataOn
        ) {
            Text(text = "CLEAN DATABASE!!!")
        }

        val context = LocalContext.current
        Button(onClick = {
            // Launch Raylib activity
            context.startActivity(Intent(context, RaylibActivity::class.java))
        }) {
            Text(text = "Launch 3D visualizer")
        }
    }
}

//// ================== Bluetooth Logic ==================
//@SuppressLint("InlinedApi")
//@RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
//@Composable
//private fun BLEConnectEffect(
//    device: BluetoothDevice,
//    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
//    indications: Boolean,
//    viewModel: BleViewModel,
//) {
//    val context: Context = LocalContext.current
//
//    // Disposable -> GATT connection is tied to this composable lifetime, on exit, calls dispose function which calls the gatt.close()
//    DisposableEffect(lifecycleOwner, device) {
//
//        val callback = viewModel.getBleCallback()
//
//        val observer = LifecycleEventObserver { _, event ->
//            if (event == Lifecycle.Event.ON_START) {
//                if (viewModel.getGattStatus() != null) {
//                    // If we previously had a GATT connection let's reestablish it
//                    viewModel.getGattStatus()?.connect()
//                } else {
//                    // Otherwise create a new GATT connection
//                    viewModel.updateGattConnection(gatt = device.connectGatt(context, false, callback))
//                }
//            }
//            if (event == Lifecycle.Event.ON_STOP) {
//                // Unless you have a reason to keep connected while in the bg you should disconnect
//                viewModel.getGattStatus()?.disconnect()
//            } else if (event == Lifecycle.Event.ON_DESTROY) {
//                viewModel.getGattStatus()?.close()
//                viewModel.resetState()
//            }
//        }
//
//        // Add the observer to the lifecycle
//        lifecycleOwner.lifecycle.addObserver(observer)
//
//        // When the effect leaves the Composition, remove the observer and close the connection
//        onDispose {
//            lifecycleOwner.lifecycle.removeObserver(observer)
//            viewModel.getGattStatus()?.close()
//            viewModel.resetState()
//        }
//    }
//}
