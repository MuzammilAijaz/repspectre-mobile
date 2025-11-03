package com.example.esp32_mpu6050_mobile_data_collection.data

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresPermission
import com.example.esp32_mpu6050_mobile_data_collection.service.AppService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

interface BleRepository {
    val applicationContext: Context
}

class AppBleRepository(
    override val applicationContext: Context
) : BleRepository {
    private val _messages: SharedFlow<String?> = AppService.messages
    val messages: SharedFlow<String?> = _messages

//    init {
//        // Coroutine scope to always collect the latest messages (contains data)
//        CoroutineScope(Dispatchers.IO).launch {
//            messages.collect { msg ->
//                if (msg != null) {
//                    message = msg
//                    Log.d("BLE Data", "Got Data")
//                }
//            }
//        }
//    }

    // --------------------------------------------------------------
    //                   Repository <-> Service
    // --------------------------------------------------------------

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

            CoroutineScope(Dispatchers.IO).launch {
                // Get the latest response object
                mService?.responses?.collect { response ->
                    _responses.emit(response)
                }
            }
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            mBound = false
        }
    }

    fun bindToService() {
        val intent = Intent(applicationContext, AppService::class.java)
        applicationContext.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    fun unbindService() {
        if (mBound) {
            applicationContext.unbindService(connection)
            mBound = false
        }
    }

    // ------------------ Interaction with Service ------------------
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun handleControlCommand(bleCommand: BleCommand) {
        when(bleCommand) {
            is BleCommand.Control.StopBle -> mService?.destroyService()
            is BleCommand.Control.DisconnectDevice -> mService?.disconnectDevice()
            else -> {}
        }
    }

    private suspend fun handleAccessCommand(bleCommand: BleCommand) {
        when(bleCommand) {
            is BleCommand.Access.GetMtu -> mService?.getMtu()
            is BleCommand.Access.GetCharacteristic -> mService?.getConnectionState()
            else -> {}
        }
    }

    private fun handleChangeCommand(bleCommand: BleCommand) {
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
        when (bleCommand) {
            is BleCommand.Control -> handleControlCommand(bleCommand)
            is BleCommand.Access -> handleAccessCommand(bleCommand)
            is BleCommand.Change -> handleChangeCommand(bleCommand)
            else -> {
                Log.d("Arguments", "incorrect bleCommand sent")
            }
        }
    }

    // ------------------ Public Functions ------------------
    /** Returns a read-only Flow which can be observed */
    fun getMessagesFlow(): Flow<String?> {
        return messages // Returns read-only, observable
    }
}