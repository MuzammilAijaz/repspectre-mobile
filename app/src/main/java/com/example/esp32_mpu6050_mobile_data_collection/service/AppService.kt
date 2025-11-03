package com.example.esp32_mpu6050_mobile_data_collection.service

import android.Manifest
import android.app.ForegroundServiceStartNotAllowedException
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.PermissionChecker
import com.example.esp32_mpu6050_mobile_data_collection.BLE.AppBluetoothGattCallback
import com.example.esp32_mpu6050_mobile_data_collection.R
import com.example.esp32_mpu6050_mobile_data_collection.data.DeviceConnectionState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.UUID

// ------------------ Constants ------------------
// CONTAINS HARDCODED UUIDS BASED ON THE DEVICE

// Random UUID for our service known between the client and server to allow communication
val SERVICE_UUID: UUID = UUID.fromString("efcdab90-7856-3412-f0de-bc9a78563412")

// Same as the service but for the characteristic
val CHARACTERISTIC_UUID: UUID = UUID.fromString("badcfe10-3254-7698-badc-fe1032547698")

val DESCRIPTOR_UUID: UUID = UUID.fromString("00efcdab-8967-4523-01ef-8967cd45ab23")

// =====================================================================================
// |                                   Service
// -------------------------------------------------------------------------------------
// |
// |
// =====================================================================================

class AppService: Service() {
    // --------------------------------------------------------------
    //                              Data
    // --------------------------------------------------------------

    // ------------------ Binder ------------------
    private val binder = LocalBinder()
    inner class LocalBinder : Binder() {
        fun getService(): AppService = this@AppService
    }

    private lateinit var myData: MyData
    data class MyData(
        @Volatile var x: Float = 1.0f,
        @Volatile var y: Float = 1.0f,
        @Volatile var z: Float = 1.0f
    )

    // ------------------ Bluetooth ------------------
    private val appBluetoothGattCallback = AppBluetoothGattCallback()
    private var device: BluetoothDevice? = null

    companion object bleStateProvider {
        data class BleState(
            val device: BluetoothDevice? = null,
            var connectionState: DeviceConnectionState = DeviceConnectionState.None
        )

        private val _messages = MutableSharedFlow<String?>(
            replay = 0,            // don’t replay old values
            extraBufferCapacity = 64 // small buffer for backpressure
        )

        val state = BleState()
        var messages: SharedFlow<String?> = _messages  // collectors will access Flow using this var

        fun updateConnection(
            gatt: BluetoothGatt? = state.connectionState.gatt,
            connectionState: Int = state.connectionState.connectionState,
            mtu: Int = state.connectionState.mtu,
            services: List<BluetoothGattService> = state.connectionState.services,
            messageSent: Boolean = state.connectionState.messageSent,
            messageReceived: String = state.connectionState.messageReceived
        ) {
            state.connectionState = state.connectionState.copy(gatt, connectionState, mtu, services, messageSent, messageReceived)

            // Emit message updates to all observers
            _messages.tryEmit(messageReceived) // try-emit: non-suspending
        }

        fun subscribeToFlow(): SharedFlow<String?> {
            return messages
        }
    }

    // --------------------------------------------------------------
    //               Public Functions to control Service
    // --------------------------------------------------------------
    public fun getCallback() : AppBluetoothGattCallback {
        return appBluetoothGattCallback
    }

    // ------------------ Control Interface ViewModel ------------------
    private val _responses = MutableSharedFlow<BleResponse>()
    val responses: SharedFlow<BleResponse> = _responses.asSharedFlow() // readonly

    sealed class BleResponse {
        // Connection / Lifecycle
        data class ConnectionState(val state: DeviceConnectionState?) : BleResponse()
        object ServiceDestroyed : BleResponse()
        object DeviceDisconnected : BleResponse()

        // Access results
        data class CharacteristicFound(val characteristic: BluetoothGattCharacteristic?) : BleResponse()
        data class Mtu(val mtu: Int) : BleResponse()

        // Errors
        data class Error(val message: String) : BleResponse()
    }

    // TODO: control interface so viewModel can request for changes in
    //  BLE connection, ask for details, change mtu etc..

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public fun destroyService() {
        onDestroy()
    }
    public fun disconnectDevice() {
        state.connectionState = DeviceConnectionState.None
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    suspend fun getCharacteristic() {
        val service = state.connectionState.gatt?.getService(SERVICE_UUID)

        _responses.emit(
            BleResponse.CharacteristicFound(
                service?.getCharacteristic(CHARACTERISTIC_UUID))
        )
    }

    suspend fun getMtu() {
        val mtu = state.connectionState.mtu
        _responses.emit(
            BleResponse.Mtu(mtu)
        )
    }

    suspend fun getConnectionState() {
        _responses.emit(
            BleResponse.ConnectionState(state.connectionState)
        )
    }

    fun changeMtu(mtu: Int) {
        updateConnection(mtu = mtu)
    }

    // --------------------------------------------------------------
    //                           Overrides
    // --------------------------------------------------------------

    private fun startForeground() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "CHANNEL_ID",
                "App Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        // ------------------ 1. Check Permission ------------------
        // Before starting the service as foreground check that the app has the
        // appropriate runtime permissions.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            // Prompt the user for Permission
            val permission =
                PermissionChecker.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)

            // CHeck if the user has granted the permission
            if (permission != PermissionChecker.PERMISSION_GRANTED) {

                // Without permissions the service cannot run in the foreground
                // Consider informing user or updating your app UI if visible.
                Log.d("AppService", "Permissions not set properly!")
                stopSelf()
                return
            }
        }

        // ------------ 2. Call startForeground with permission ------------
        try {
            val notification = NotificationCompat.Builder(this, "CHANNEL_ID")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                // Create the notification to display while the service is running
                .build()
            ServiceCompat.startForeground(
                /* service = */ this,
                /* id = */ 100, // Cannot be 0
                /* notification = */ notification,
                /* foregroundServiceType = */
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                } else {
                    0
                },
            )
        } catch (e: Exception) {
            Log.d("AppService", "Exception: Crashed")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    && e is ForegroundServiceStartNotAllowedException
            ) {
                // App not in a valid state to start foreground service
                // (e.g. started from bg)
            }
            // ...
        }
    }

    override fun onCreate() {
        myData = MyData()
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground() // upgrade the service to foreground service

        // The device object is passed using the Intent.apply {}
        device = intent?.getParcelableExtra("BLE_DEVICE", BluetoothDevice::class.java)

        if (state.connectionState.gatt != null) {
            state.connectionState.gatt?.connect()
        } else {
            state.connectionState.copy(gatt = device?.connectGatt(this, false, appBluetoothGattCallback))
        }

        return START_STICKY // Restarts service if service gets killed
    }

    override fun onBind(p0: Intent?): IBinder? {
        return binder
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onDestroy() {
        Log.d("AppService", "App Service DESTROYED!!")
        state.connectionState.gatt?.disconnect()
        state.connectionState.gatt?.close()
        state.connectionState = DeviceConnectionState.None
    }


}