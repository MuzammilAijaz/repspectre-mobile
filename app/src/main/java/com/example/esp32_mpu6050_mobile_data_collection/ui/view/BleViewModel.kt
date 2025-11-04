package com.example.esp32_mpu6050_mobile_data_collection.ui.view

import android.Manifest
import android.annotation.SuppressLint
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.esp32_mpu6050_mobile_data_collection.data.AppBleRepository
import com.example.esp32_mpu6050_mobile_data_collection.data.BleCommand
import com.example.esp32_mpu6050_mobile_data_collection.data.SensorApplication
import com.example.esp32_mpu6050_mobile_data_collection.service.AppService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

const val DEVICE_MTU = 200 // TODO: handle this better

@SuppressLint("MissingPermission")
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

    // ----- State --------------------------------------------------
    data class BleUIState(
         val bleState: AppService.BleStateProvider.BleState = AppService.BleStateProvider.BleState()
    )

    private val _uiState: MutableStateFlow<BleUIState> = MutableStateFlow(BleUIState())
    public val uiState: StateFlow<BleUIState> = _uiState.asStateFlow()

    // ----- Init ---------------------------------------------------
    init {
        viewModelScope.launch {
            handleResponse()
            repository.responses.collectLatest { response ->
                _responses.value = response
            }
        }

        // Coroutine responsible for collecting the latest message
        viewModelScope.launch {
            repository.bleState.collectLatest { bleState ->
                Log.d("ViewModel", "New bleState: ${bleState.connectionState}")
                _uiState.update { it.copy(bleState = bleState) }
            }
            Log.d("ViewModel", "Got Ble State")
        }
    }

    // --------------------------------------------------------------
    //                           Functions
    // --------------------------------------------------------------

    // ----- Control Service from UI --------------------------------
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun handleCommand(bleCommand: BleCommand) {
        Log.d("ViewModel", "Inside HandleCommand")
        viewModelScope.launch {
            Log.d("ViewModel", "Inside coRoutine")
            // All the commands are handled by the Repository
            repository.handleCommand(bleCommand = bleCommand)
        }
    }

    private fun handleResponse() {
        // TODO: Repository should handle all the responses instead

        viewModelScope.launch {
            responses.collect { response ->
                // TODO: handle all responses
            }
        }
    }


    // --------------------------------------------------------------
    //                        Public Function
    // --------------------------------------------------------------

    public fun repositoryBindToService() {
        repository.bindToService()
    }

    // --------------------------------------------------------------
    //                        User Commands
    // --------------------------------------------------------------
    // Used when the User requires any changes on the connection state or
    //  device state via UI interaction.

    // ----- Access Methods -----------------------------------------
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public fun getConnectionState() {
        handleCommand(BleCommand.Access.GetConnectionState)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public fun getMtu() {
        handleCommand(BleCommand.Access.GetMtu)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public fun readCharacteristic() {
        handleCommand(BleCommand.Access.ReadCharacteristic)
    }

    // ----- Change Methods -----------------------------------------
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public fun changeMtu() {
        handleCommand(BleCommand.Change.ChangeMtu(DEVICE_MTU))
    }

    // ----- Control Methods ----------------------------------------
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public fun discoverServices() {
        handleCommand(BleCommand.Control.DiscoverServices)
    }

    public fun disconnectGatt() {
        handleCommand(BleCommand.Control.StopBle)
    }
}
