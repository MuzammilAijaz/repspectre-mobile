package com.example.esp32_mpu6050_mobile_data_collection.data

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresPermission
import com.example.esp32_mpu6050_mobile_data_collection.raylib.updateNativeOrientation
import com.example.esp32_mpu6050_mobile_data_collection.service.AppService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

interface BleRepository {
    val applicationContext: Context
}

class AppBleRepository(
    override val applicationContext: Context
) : BleRepository {

    // --------------------------------------------------------------
    //                   Repository <-> Service
    // --------------------------------------------------------------
    // SHARES THE WHOLE BLE STATE, INCLUDING MESSAGES (VERY BIG)!!!
    private val _bleState = MutableStateFlow(AppService.BleStateProvider.BleState())
    val bleState: StateFlow<AppService.BleStateProvider.BleState> = _bleState.asStateFlow()

    // High-frequency sensor data — SharedFlow instead of StateFlow
    private val _sensorFlow = MutableSharedFlow<AppService.SensorData>(
        extraBufferCapacity = 512,                  // buffer so collectors can keep up
        onBufferOverflow = BufferOverflow.DROP_OLDEST // drop oldest if full
    )
    val sensorFlow: Flow<AppService.SensorData> = _sensorFlow.asSharedFlow()

    // ------------------ Binding to Service ------------------
    private val _responses = MutableSharedFlow<AppService.BleResponse>()
    val responses: SharedFlow<AppService.BleResponse> = _responses

    private var mService: AppService? = null
    private var mBound: Boolean = false

    /** Defines callbacks for service binding, passed to bindService().  */
    private val connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance.
            val binder = service as AppService.LocalBinder
            mService = binder.getService()
            mBound = true

            // ----- Collectors for updating Flow and passing it on ---------
            // TODO: NOT IMPLEMENTED
            CoroutineScope(Dispatchers.IO).launch {
                // Get the latest response object
                mService?.responses?.collect { response ->
                    _responses.emit(response)
                }
            }
            // Collect low freq UI information (heavy and slow)
            CoroutineScope(Dispatchers.IO).launch {
                mService?.bleState?.collect { state ->
                    //Log.d("RepositoryValues", "Accel: x=${state.connectionState.messageReceived.x} y=${state.connectionState.messageReceived.y} z=${state.connectionState.messageReceived.z}, z=${state.connectionState.messageReceived.w}")
                    _bleState.value = state
                }
            }
            // Collect raw sensor data (high frequency)
            CoroutineScope(Dispatchers.IO).launch {
                mService?.sensorFlow?.collect { data ->
                    Log.d("RepositoryValues", "Accel: x=${data.x} y=${data.y} z=${data.z}, z=${data.w}")
                    _sensorFlow.tryEmit(data)  // non-blocking
                }
            }

            // ----- Passing data to native side ----------------------------
            CoroutineScope(Dispatchers.IO).launch {
                _bleState.collectLatest { state ->
                    val quaternions = _bleState.value.connectionState.messageReceived
                    updateNativeOrientation(quaternions.x, quaternions.y, quaternions.z, quaternions.w?:0f)
                }
            }
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            mBound = false
        }
    }

    // TODO: NOTE(code-duplication) -> this function was copied from AppViewModel...
    /** Returns List in form of : x, y, z */
    private fun parseString(string: String): List<Float>{
        val input = string

        val regex = Regex("""x=([-+]?\d*\.?\d+)\s+y=([-+]?\d*\.?\d+)\s+z=([-+]?\d*\.?\d+)""")
        val match = regex.find(input)

        if (match != null) {
            val (x, y, z) = match.destructured
            val xFloat = x.toFloat()
            val yFloat = y.toFloat()
            val zFloat = z.toFloat()
            return listOf(xFloat,yFloat,zFloat)
        }
        else {
            return listOf(0f, 0f, 0f)
        }
    }

    fun bindToService() {
        val intent = Intent(applicationContext, AppService::class.java)
        applicationContext.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    fun unbindService() { // TODO: implement this
        if (mBound) {
            applicationContext.unbindService(connection)
            mBound = false
        }
    }

    // ------------------ Interaction with Service ------------------
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun handleControlCommand(bleCommand: BleCommand) {

        Log.d("ServiceOnClose", "Command is for: $bleCommand")
        when(bleCommand) {
            is BleCommand.Control.StopBle -> mService?.destroyService() // questionable, kills the whole service, probably remove this!!!!
            is BleCommand.Control.DisconnectDevice -> mService?.disconnectDevice()
            else -> {}
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private suspend fun handleAccessCommand(bleCommand: BleCommand) {
        when(bleCommand) {
            is BleCommand.Access.GetMtu -> mService?.getMtu()
            is BleCommand.Access.GetCharacteristic -> mService?.getConnectionState()
            is BleCommand.Access.ReadCharacteristic -> mService?.readCharacteristic()
            else -> {}
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private suspend fun handleChangeCommand(bleCommand: BleCommand) {
        when(bleCommand) {
            is BleCommand.Change.ChangeMtu -> mService?.changeMtu(bleCommand.mtu)
            else -> {}
        }
    }

    // --------------------------------------------------------------
    //                  ViewModel <-> Repository
    // --------------------------------------------------------------

    // ------------- Command interface to service -------------
    // TODO: functionality to allow viewmodel to send COMMANDS
    //  to the service in order to START, STOP, RECONNECT, GET Details etc.
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    suspend fun handleCommand(bleCommand: BleCommand) {
        Log.d("ServiceOnClose", "inside suspend function inside repo")
        Log.d("ViewModel", "Inside coRoutine")
        when (bleCommand) {
            is BleCommand.Control -> {
                Log.d("ServiceOnClose", "Dispatching to Control handler")
                handleControlCommand(bleCommand)
            }
            is BleCommand.Access -> handleAccessCommand(bleCommand)
            is BleCommand.Change -> handleChangeCommand(bleCommand)
            else -> {
                Log.d("Arguments", "incorrect bleCommand sent")
            }
        }
    }
}