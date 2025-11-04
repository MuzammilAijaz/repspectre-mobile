package com.example.esp32_mpu6050_mobile_data_collection.ui.screen

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattDescriptor
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.esp32_mpu6050_mobile_data_collection.BLE.BluetoothSampleBox
import com.example.esp32_mpu6050_mobile_data_collection.BLE.FindDevicesScreen
import com.example.esp32_mpu6050_mobile_data_collection.raylib.RaylibActivity
import com.example.esp32_mpu6050_mobile_data_collection.service.AppService
import com.example.esp32_mpu6050_mobile_data_collection.service.CHARACTERISTIC_UUID
import com.example.esp32_mpu6050_mobile_data_collection.service.SERVICE_UUID
import com.example.esp32_mpu6050_mobile_data_collection.ui.view.AppViewModel
import com.example.esp32_mpu6050_mobile_data_collection.ui.view.BleViewModel
import java.util.UUID

// =====================================================================================
// |                                Main Composable
// =====================================================================================

@SuppressLint("MissingPermission")
@OptIn(ExperimentalAnimationApi::class)
@RequiresApi(Build.VERSION_CODES.M)
@Composable
fun ConnectGATTSample(
    selectedDevice: BluetoothDevice?,
    appViewModel: AppViewModel,
    onSelectedDeviceChange: (BluetoothDevice?) -> Unit,
    onDeviceFound: () -> Unit,
) {
//    var selectedDevice by remember {
//        mutableStateOf<BluetoothDevice?>(null)
//    }
    // Check that BT permissions and that BT is available and enabled
    BluetoothSampleBox {
        AnimatedContent(targetState = selectedDevice, label = "Selected device") { device ->
            if (device == null) {
                // Scans for BT devices and handles clicks (see FindDeviceSample)
                FindDevicesScreen {
                    onSelectedDeviceChange(it)
                }
            } else {
                // Once a device is selected show the UI and try to connect device
                onDeviceFound()
            }
        }
    }
}

// =====================================================================================
// |                                     UI
// =====================================================================================
// | NOTES AND CONSIDERATIONS:
// |    -> right now implementation requires duplicate of code in :
// |        -> stopping the saving of data to the database
// |        -> Binding of Repository probably shouldn't be done from here
// |
// =====================================================================================
@SuppressLint("InlinedApi")
@RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
@Composable
fun ConnectDeviceScreen(device: BluetoothDevice, appViewModel: AppViewModel, onDatabaseShowButtonClick: () -> Unit, onClose: () -> Unit) {
    val viewModel: BleViewModel = viewModel(factory = BleViewModel.Factory)

    var isStoreDataOn: Boolean by remember { mutableStateOf(false) }
    var isNewSession: Boolean by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsState()
    val bleState = uiState.bleState

    // TODO: Re-implement this
    var indications by remember { mutableStateOf(false) }

    // ----- Start Service and Bind Repository to Service ---------
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        // This service will handle the connection and update the BleRepository
        val intent = Intent(context, AppService::class.java).apply {
            putExtra("BLE_DEVICE", device) // pass in extra arguments inside the intent
        }
        context.startService(intent)
        viewModel.repositoryBindToService()
    }
    // --------------------------------------------------------------

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = "Devices details", style = MaterialTheme.typography.headlineSmall)
        Text(text = "Name: ${device.name} (${device.address})")

        Text(text = "Status: ${bleState.connectionState.connectionState}")
        Text(text = "MTU: ${bleState.connectionState.mtu}")
        Text(text = "Services: ${bleState.connectionState.services.joinToString { it.uuid.toString() + " " + it.type }}")
        Text(text = "Message sent: ${bleState.connectionState.messageSent}")

        val message: AppService.SensorData = bleState.connectionState.messageReceived

        Log.d("Sensor Data", "Quaternions: ${message.x} ${message.y} ${message.z} ${message.w}")
        Text(text = "Quaternions: ${message.x} ${message.y} ${message.z} ${message.w}")

        if (isStoreDataOn) {
            if(isNewSession) {appViewModel.createNewSession() ; isNewSession = false}

            Log.d("Database", "Collection Started")

            appViewModel.insertQuaternionValue(message)
        }

        Button(
            onClick = {
                // TODO: have user change MTU and pass value
                viewModel.changeMtu()
            },
        ) {
            Text(text = "Request MTU")
        }

        Button(
            enabled = bleState.connectionState.connectionState == 2,
            onClick = {
                // Once we have the connection discover the peripheral services
                viewModel.discoverServices()
            },
        ) {
            Text(text = "Discover")
        }

        // TODO: Write functionality
//        Button(
////            enabled = state?.gatt != null && characteristic != null,
//            enabled = uiState.value.connectionState,
//            onClick = {
//                scope.launch(Dispatchers.IO) {
//                    sendData(state?.gatt!!, characteristic!!)
//                }
//            },
//        ) {
//            Text(text = "Write to server")
//        }

        Button(
            enabled = bleState.connectionState.connectionState == 2,
            onClick = {
                viewModel.readCharacteristic()
            },
        ) {
            Text(text = "Read characteristic")
        }
// ----- --------------------------------------------------------// ----- --------------------------------------------------------
        // TODO: Re-Implement and Verify all these composables
        Button(
            onClick = {
                indications = !indications
                Log.d("BLE Data", "Indications: $indications")

                val characteristic = bleState.connectionState.gatt
                    ?.getService(SERVICE_UUID)
                    ?.getCharacteristic(CHARACTERISTIC_UUID)

                if (characteristic != null) {
                    bleState.connectionState.gatt?.setCharacteristicNotification(characteristic, indications)

                    val descriptor = characteristic.getDescriptor(
                        UUID.fromString("00002902-0000-1000-8000-00805F9B34FB")
                    )
                    descriptor?.value = if (indications)
                        BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
                    else
                        BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE

                    Log.d("BLE Data", "${descriptor?.value}")

                    bleState.connectionState.gatt?.writeDescriptor(descriptor)
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
                if(!isStoreDataOn) {
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
                if(isStoreDataOn) {
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
            viewModel.disconnectGatt()
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

        var showDatabase: Boolean by remember {mutableStateOf(false)}
        Button(onClick = onDatabaseShowButtonClick ) {
            Text(text = "Show Database")
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