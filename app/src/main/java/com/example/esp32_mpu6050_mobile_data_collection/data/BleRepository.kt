package com.example.esp32_mpu6050_mobile_data_collection.data

import android.R.id.message
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.example.esp32_mpu6050_mobile_data_collection.service.AppService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

interface BleRepository {
    val applicationContext: Context
}

class AppBleRepository(
    override val applicationContext: Context
) : BleRepository {
    val messages = AppService.messages

    init {
        CoroutineScope(Dispatchers.IO).launch {
            messages.collect { msg ->
                if (msg != null) {
                    message = msg
                    Log.d("BLE Data", "Got Data")
                }
            }
        }
    }

    // --------------------------------------------------------------
    //                   Repository <-> Service
    // --------------------------------------------------------------

    // ------------------ Binding to Service ------------------
    private var mService: AppService? = null
    private var mBound: Boolean = false

    /** Defines callbacks for service binding, passed to bindService().  */
    private val connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance.
            val binder = service as AppService.LocalBinder
            mService = binder.getService()
            mBound = true
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

    // --------------------------------------------------------------
    //                  ViewModel <-> Repository
    // --------------------------------------------------------------

    // ------------- Command interface to service -------------
    // TODO: functionality to allow viewmodel to send COMMANDS
    //  to the service in order to START, STOP, RECONNECT, GET Details etc.

    // ------------------ Public Functions ------------------
    /** Returns a read-only Flow which can be observed */
    fun getMessagesFlow(): Flow<String?> {
        return messages // Returns read-only, observable
    }
}