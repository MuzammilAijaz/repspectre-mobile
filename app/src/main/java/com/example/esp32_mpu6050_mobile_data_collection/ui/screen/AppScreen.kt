package com.example.esp32_mpu6050_mobile_data_collection.ui.screen

import android.Manifest
import android.bluetooth.BluetoothDevice
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.esp32_mpu6050_mobile_data_collection.ui.view.AppViewModel

enum class ScreenRoutes(name: String) {
    BluetoothSelectionScreen("BluetoothScreen"),
    BluetoothDeviceScreen("BleDeviceSCreen"),
    DataScreen("DataScreen")
}

@RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
@Composable
fun AppScreen() {
    val navController = rememberNavController()

    val appViewModel: AppViewModel = viewModel(factory = AppViewModel.Factory)

    var selectedDevice by remember {
        mutableStateOf<BluetoothDevice?>(null)
    }

    val onSelectedDeviceChange: (BluetoothDevice?) -> Unit = { device ->
        selectedDevice = device
    }

    NavHost(navController = navController, startDestination = ScreenRoutes.BluetoothSelectionScreen.name) {
        composable(route = ScreenRoutes.BluetoothSelectionScreen.name) {
            ConnectGATTSample(
                selectedDevice = selectedDevice,
                appViewModel = appViewModel,
                onSelectedDeviceChange = onSelectedDeviceChange,
            ) {
                navController.navigate(ScreenRoutes.BluetoothDeviceScreen.name)
            }
        }
        composable(route = ScreenRoutes.BluetoothDeviceScreen.name) {
            ConnectDeviceScreen(
                device = selectedDevice as BluetoothDevice,
                appViewModel = appViewModel,
                onDatabaseShowButtonClick = {
                    navController.navigate(ScreenRoutes.DataScreen.name)
                },
            ) {
                onSelectedDeviceChange(null)
            }
        }
        composable(route = ScreenRoutes.DataScreen.name) {
            EntityDataScreen(
                appViewModel.getAllQuaternions().collectAsState(initial = emptyList()).value
            ) {
                navController.navigate(ScreenRoutes.BluetoothDeviceScreen.name)
            }
        }

    }
}
