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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
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

    // ------------------ Binding to Service ------------------
    private val _responses = MutableSharedFlow<AppService.BleResponse>()
    val responses: SharedFlow<AppService.BleResponse> = _responses

    private var mService: AppService? = null
    private var mBound: Boolean = false

    private val _bleState = MutableStateFlow(AppService.BleStateProvider.BleState())
    val bleState: StateFlow<AppService.BleStateProvider.BleState> = _bleState.asStateFlow()

    /** Defines callbacks for service binding, passed to bindService().  */
    private val connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance.
            val binder = service as AppService.LocalBinder
            mService = binder.getService()
            mBound = true

            // ----- Collectors for updating Flow and passing it on ---------
            CoroutineScope(Dispatchers.IO).launch {
                // Get the latest response object
                mService?.responses?.collect { response ->
                    _responses.emit(response)
                }
            }
            CoroutineScope(Dispatchers.Default).launch {
                mService?.bleState?.collect { state ->
                    _bleState.value = state
                }
            }
            // ----- Passing data to native side ----------------------------
            CoroutineScope(Dispatchers.Default).launch {
                _bleState.collectLatest { state ->
                    val quaternions = _bleState.value.connectionState.messageReceived
                    // ESP SENDS :
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