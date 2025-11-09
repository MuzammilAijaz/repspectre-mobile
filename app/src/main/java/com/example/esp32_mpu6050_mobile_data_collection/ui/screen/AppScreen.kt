package com.example.esp32_mpu6050_mobile_data_collection.ui.screen

import android.Manifest
import android.bluetooth.BluetoothDevice
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
                onSelectedDeviceChange = onSelectedDeviceChange,
            ) {
                navController.navigate(ScreenRoutes.BluetoothDeviceScreen.name)
            }
        }
        composable(route = ScreenRoutes.BluetoothDeviceScreen.name) {
            // android holds the old screen until recomposition is fully done, so we have to manage
            // the case where user pressed the close button in this screen, leading to selectedDevice
            // being read as Null

            val device = selectedDevice as? BluetoothDevice
            if (device != null) {
                ConnectDeviceScreen(
                    device = device,
                    appViewModel = appViewModel,
                    onDatabaseShowButtonClick = {
                        navController.navigate(ScreenRoutes.DataScreen.name)
                    },
                ) {
                    onSelectedDeviceChange(null)
                    navController.navigate(ScreenRoutes.BluetoothSelectionScreen.name)
                }
            }
            else {
                // LaunchedEffect to ensure they run after composition
                LaunchedEffect(Unit) {
                    navController.navigate(ScreenRoutes.BluetoothSelectionScreen.name) {
                        popUpTo(ScreenRoutes.BluetoothDeviceScreen.name) { inclusive = true }
                    }
                }
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
