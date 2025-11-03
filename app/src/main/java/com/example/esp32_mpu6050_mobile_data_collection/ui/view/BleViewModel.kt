package com.example.esp32_mpu6050_mobile_data_collection.ui.view

import android.Manifest
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.esp32_mpu6050_mobile_data_collection.data.AppBleRepository
import com.example.esp32_mpu6050_mobile_data_collection.data.AppContainer
import com.example.esp32_mpu6050_mobile_data_collection.data.BleCommand
import com.example.esp32_mpu6050_mobile_data_collection.data.BleRepository
import com.example.esp32_mpu6050_mobile_data_collection.data.SensorApplication
import com.example.esp32_mpu6050_mobile_data_collection.data.appBleRepositoryProvider
import com.example.esp32_mpu6050_mobile_data_collection.service.AppService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

// =====================================================================================
// |                        ViewModel: Control and manage
// -------------------------------------------------------------------------------------
// |
// |
// =====================================================================================

class BleViewModel(
    val repository: AppBleRepository
) : ViewModel() {

    // ----- Factory ------------------------------------------------
    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as SensorApplication)
                val repository = application.container.bleRepository
                BleViewModel(repository)
            }
        }
    }

    // ----- Response -----------------------------------------------
    private val _responses = MutableStateFlow<AppService.BleResponse?>(null)
    val responses: StateFlow<AppService.BleResponse?> = _responses

    // ----- BLE Messages (data values) -----------------------------
    /** Holds and caches the latest Message only, updated with change in Flow
     * Survives configuration changes, as its MutableStateFlow */
    private val _latestMessage = MutableStateFlow<String?>(null)

    val latestMessage: StateFlow<String?> = _latestMessage.asStateFlow() // Read only Flow

    // ----- Init ---------------------------------------------------
    init {
        // Coroutine responsible for collecting the latest message
        viewModelScope.launch {
            repository.getMessagesFlow().collectLatest { message ->
                _latestMessage.value = message
            }
        }
    }

    // --------------------------------------------------------------
    //                           Functions
    // --------------------------------------------------------------

    // ----- Control Service from UI --------------------------------
    // TODO: implement functions to allow user to control state of Service
    public fun handleCommand(bleCommand: BleCommand) {
        viewModelScope.launch {
            repository.handleCommand(bleCommand = bleCommand)
        }
    }

    public fun handleResponse() {
        viewModelScope.launch {
            responses.collect { response ->
                when(reponse) {
                    is AppService.BleResponse.
                }
            }
        }
    }

    // ----- Old API ------------------------------------------------
    // TODO: remove/ update them

    data class BleUIState(
        val connectionState: String = "None",
        val

    )

    private val _uiState: BleUIstate = BleUIState()

    public fun getConnectionState() {
        handleCommand(BleCommand.Access.)
    }

    public fun getBleCallback() : BluetoothGattCallback {
        if (mBound) {
            return connection.
        }
    }

    public fun getGattStatus(): BluetoothGatt? {return _uiState.value.connectionState.gatt}

    public fun updateGattConnection(gatt: BluetoothGatt) {
        _uiState.update { currentState ->
            currentState.copy(connectionState = currentState.connectionState.copy(gatt = gatt))
        }
    }

    public fun resetState() {
        _uiState.value = BluetoothUiState()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public fun disconnectAndClose() {
        Appservice
        _uiState.apply {
            try {
                _uiState.value.connectionState.gatt?.disconnect()
                _uiState.value.connectionState.gatt?.close()
            } catch (e: Exception) {
                Log.e("BLE", "Error while closing GATT: ${e.message}")
            }
        }
        resetState()
    }
}
