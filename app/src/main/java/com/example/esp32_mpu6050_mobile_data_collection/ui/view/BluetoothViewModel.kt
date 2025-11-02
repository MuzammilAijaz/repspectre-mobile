package com.example.esp32_mpu6050_mobile_data_collection.ui.view

import android.Manifest
import android.app.Application
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import com.example.esp32_mpu6050_mobile_data_collection.data.DeviceConnectionState
import com.example.esp32_mpu6050_mobile_data_collection.service.AppService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.math.RoundingMode
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.DecimalFormat
import kotlin.jvm.java

class BluetoothViewModel(application: Application) : AndroidViewModel(application) {

    // ------------------ Binding to the Service ------------------
    private val context = getApplication<Application>()
    private var mBound: Boolean = false
    private var appService: AppService? = null

    /** Defines callbacks for service binding, passed to bindService().  */
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance.
            val binder = service as? AppService.LocalBinder
            appService = binder?.getService()
            mBound = appService != null
        }
        override fun onServiceDisconnected(arg0: ComponentName) {
            appService = null
            mBound = false
        }
    }

    init {
        // Bind the calling Activity (in this case, MainActivity) to the AppService
        val intent = Intent(context, AppService::class.java)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    // -------------------------------------------------------------

    // Keep the current connection state
    data class BluetoothUiState(
        val connectionState: DeviceConnectionState = DeviceConnectionState.None,
        val device: BluetoothDevice? = null
    )
    private val _uiState = MutableStateFlow(BluetoothUiState())
    public val uiState = _uiState.asStateFlow()

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
