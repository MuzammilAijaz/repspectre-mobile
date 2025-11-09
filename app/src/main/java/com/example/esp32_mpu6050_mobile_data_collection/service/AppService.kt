package com.example.esp32_mpu6050_mobile_data_collection.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.ForegroundServiceStartNotAllowedException
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.PermissionChecker
import com.example.esp32_mpu6050_mobile_data_collection.BLE.AppBluetoothGattCallback
import com.example.esp32_mpu6050_mobile_data_collection.R
import com.example.esp32_mpu6050_mobile_data_collection.data.DeviceConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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

    /* This was meant to be sent to the native layer*/
    private lateinit var myData: MyData
    data class MyData(
        @Volatile var x: Float = 1.0f,
        @Volatile var y: Float = 1.0f,
        @Volatile var z: Float = 1.0f
    )

    // ------------------ Binder ------------------
    private val binder = LocalBinder()
    inner class LocalBinder : Binder() {
        fun getService(): AppService = this@AppService
    }

    // ------------------ Bluetooth ------------------
    private val appBluetoothGattCallback = AppBluetoothGattCallback(this)
    private var device: BluetoothDevice? = null
    private var bluetoothGatt: BluetoothGatt? = null

    private var isConnecting = false

    private val _bleState =  MutableStateFlow(BleState())
    val bleState = _bleState.asStateFlow() // public, read-only, read by repository -> viewModel

    companion object BleStateProvider {
        data class BleState(
            val device: BluetoothDevice? = null,
            var connectionState: DeviceConnectionState = DeviceConnectionState.None
        )
    }
    // ------------------ Handler and Scope ------------------
    private val handler = Handler(Looper.getMainLooper())
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    public var isServiceDestructionRequired: Boolean = false

    // --------------------------------------------------------------
    //                        Private Functions
    // --------------------------------------------------------------

    /** Fully destroys the service. */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun resetConnection() {
        isServiceDestructionRequired = true

        // after .disconnect() is done, onConnectionChange callback is called, which is use to close the connection fully
        val gatt = _bleState.value.connectionState.gatt
        if (gatt != null) {
            Log.d("AppService", "Requesting disconnection...")

            // Collision avoidance
            handler.removeCallbacksAndMessages(null)  // cancel any delayed tasks
            coroutineScope.cancel()  // cancel any ongoing coroutines to avoid

            // REQUEST for disconnection; actual disconnection done by gatt.close() in onConnectionChanged callback
            unsubscribeAndDisableDeviceNotification(gatt)
            Log.d("ServiceOnClose", "resetting connectiong")
        }
    }
    // onConnectionChange callback calls this function
    // clearing the bleState object of the app
    public fun resetBleState() {
        _bleState.value = BleState(null, DeviceConnectionState.None)
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
        data class Service(val service: BluetoothGattService?) : BleResponse()
        object CharacteristicRead : BleResponse()

        // Change results
        data class MtuChanged(val mtu: Int) : BleResponse()

        // Errors
        data class Error(val message: String) : BleResponse()
    }

    // TODO: control interface so viewModel can request for changes in
    //  BLE connection, ask for details, change mtu etc..

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public fun destroyService() {
        onDestroy()
    }
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public fun disconnectDevice() {
        Log.d("ServiceOnClose", "Disconnecting from device")
        resetConnection()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    suspend fun getCharacteristic() {
        val service = _bleState.value.connectionState.gatt?.getService(SERVICE_UUID)

        _responses.emit(
            BleResponse.CharacteristicFound(
                service?.getCharacteristic(CHARACTERISTIC_UUID))
        )
    }

    suspend fun getMtu() {
        val mtu = _bleState.value.connectionState.mtu
        _responses.emit(
            BleResponse.Mtu(mtu)
        )
    }

    suspend fun getService() {
        _responses.emit(
            BleResponse.Service(_bleState.value.connectionState.gatt?.getService(SERVICE_UUID))
        )
    }

    suspend fun getConnectionState() {
        _responses.emit(
            BleResponse.ConnectionState(_bleState.value.connectionState)
        )
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    suspend fun changeMtu(mtu: Int) {
        if (_bleState.value.connectionState.gatt?.requestMtu(mtu)?: false) {
            updateConnection(mtu = mtu) // actually change the mtu
            _responses.emit(
                BleResponse.MtuChanged(mtu) // let others know
            )
            Log.d("AppService", "MTU Changed to 200")
        } else {
            Log.d("AppService", "FAILED!! CHANGING OF MTU")
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    suspend fun readCharacteristic() {
        val result = _bleState.value.connectionState.gatt?.readCharacteristic(
            _bleState.value.connectionState.gatt?.getService(SERVICE_UUID)
                ?.getCharacteristic(CHARACTERISTIC_UUID)
        )
        _responses.emit(
            BleResponse.CharacteristicRead
        )
    }

    // --------------------------------------------------------------
    //                     Used By Gatt Callback
    // --------------------------------------------------------------

    private var readJob: Job? = null
    private var isActive: Boolean = true

    @SuppressLint("MissingPermission")
    @OptIn(ExperimentalCoroutinesApi::class)
    fun subscribeToService() {
        // Cancel previous reading loop if any
        readJob?.cancel()

        readJob = CoroutineScope(Dispatchers.IO).launch {
            val gatt = _bleState.value.connectionState.gatt
            if (gatt == null) {
                Log.e("AppService", "No active GATT connection!")
                return@launch
            }

            val targetService = gatt.getService(SERVICE_UUID)
            val targetCharacteristic = targetService?.getCharacteristic(CHARACTERISTIC_UUID)

            if (targetCharacteristic == null) {
                Log.e("AppService", "Characteristic not found!")
                return@launch
            }

            Log.d("AppService", "Subscribed to: ${targetCharacteristic.uuid}")

            // Try enabling notifications first
            val notificationEnabled = gatt.setCharacteristicNotification(targetCharacteristic, true)
            val descriptor = targetCharacteristic.getDescriptor(
                UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
            )
            descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            descriptor?.let { gatt.writeDescriptor(it) }

            Log.d("AppService", "Notifications enabled: $notificationEnabled")

            // 🔁 Fallback: loop reads manually if notifications don’t come
            while (isActive) {
                try {
                    val success = gatt.readCharacteristic(targetCharacteristic)
                } catch (e: Exception) {
                    Log.e("AppService", "Read loop failed: ${e.message}")
                    break
                }
            }
        }
    }

    data class SensorData(
        val x: Float,
        val y: Float,
        val z: Float,
        val w: Float? = null,
    )

    /* VERY IMPORTANT FUNCTION:
    * Called by the Bluetooth GATT Callback functions on every State change */
    fun updateConnection(
        gatt: BluetoothGatt? = _bleState.value.connectionState.gatt,
        connectionState: Int = _bleState.value.connectionState.connectionState,
        mtu: Int = _bleState.value.connectionState.mtu,
        services: List<BluetoothGattService> = _bleState.value.connectionState.services,
        messageSent: Boolean = _bleState.value.connectionState.messageSent,
        messageReceived: SensorData = _bleState.value.connectionState.messageReceived
    ) {
        _bleState.update { old ->
            old.copy(
                connectionState = old.connectionState.copy(
                    gatt = gatt,
                    connectionState = connectionState,
                    mtu = mtu,
                    services = services,
                    messageSent = messageSent,
                    messageReceived = messageReceived
                )
            )
        }
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

        // ----- 1. Check Permission ------------------------------------
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

        // ----- 2. Call startForeground with permissions ---------------
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
                // App not in a valid _bleState.value to start foreground service
                // (e.g. started from bg)
            }
        }
    }

    override fun onCreate() {
        myData = MyData()
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        Log.d("AppServiceConnection", "Started Service, Now configuring")
        startForeground() // upgrade the service to foreground service

        // ----- Guard : avoid multiple connection instances ------------
        // If already connecting or connected, skip re-connect
        if (bluetoothGatt != null) {
            Log.d("AppService", "Already connected or connecting, ignoring duplicate startCommand()")
            return START_STICKY
        }

        if (isConnecting) {
            Log.d("AppService", "Connection already in progress, ignoring duplicate startCommand()")
            return START_STICKY
        }

        // ----- --------------------------------------------------------
        // The device object is passed using the Intent.apply {}
        @Suppress("DEPRECATION")
        device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra("BLE_DEVICE", BluetoothDevice::class.java)
        } else {
            intent?.getParcelableExtra("BLE_DEVICE")
        }

        if (device == null) {
            Log.e("AppService", "Device is null - cannot connect")
        }
        else {
            Log.d("AppService", "Calling connectGatt() for device=${device?.address}")

            isConnecting = true
            bluetoothGatt = device?.connectGatt(this@AppService, false, appBluetoothGattCallback)

            if (bluetoothGatt != null) {
                // record the returned BluetoothGatt immediately; do NOT call gatt.connect()
                Log.d("AppService", "connectGatt() returned BluetoothGatt, waiting for callbacks...")
            } else {
                Log.e("AppService", "connectGatt() returned null")
            }
        }

//        Log.d("AppService", "State: ${bleState.value.connectionState.connectionState}")
//        Log.d("AppService", "Mtu: ${bleState.value.connectionState.mtu}")
//        Log.d("AppService", "Message: ${bleState.value.connectionState.messageReceived}")
//        Log.d("AppService", "Configuring complete")
        return START_STICKY // Restarts service if service gets killed
    }

    override fun onBind(p0: Intent?): IBinder? {
        return binder
    }

    // Called when user removes the app from recents
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        isServiceDestructionRequired = true

        // Tell android to no longer subscribe to notifications
        unsubscribeAndDisableDeviceNotification(_bleState.value.connectionState.gatt) // request for disconnection

        Log.d("AppServiceDisconnection", "Service Requested Disconnection by user")
    }

    // Called when SYSTEM kills the application
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onDestroy() {
        Log.d("AppServiceDisconnection", "Service Requested Disconnection by system")
        isServiceDestructionRequired = true

        // Tell android to no longer subscribe to notifications
        unsubscribeAndDisableDeviceNotification(_bleState.value.connectionState.gatt) // request for disconnection
    }

    private fun stopReadLoop() {
        readJob?.cancel()
        isActive = false
        readJob = null
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun unsubscribeAndDisableDeviceNotification(gatt: BluetoothGatt?) {
        if (gatt == null) {
            Log.d("AppServiceDisconnection", "STOPPING BECAUSE OF GATT = NULL")
            stopService()
        }

        stopReadLoop()

        val characteristic = gatt?.getService(SERVICE_UUID)
            ?.getCharacteristic(CHARACTERISTIC_UUID)
        if (characteristic != null) {
            // Tell Android not to listen
            gatt.setCharacteristicNotification(characteristic, false)

            // Also disable on the ESP32
            val descriptor = characteristic?.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
            descriptor?.let {
                val enableNotificationByte = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
                it.value = enableNotificationByte // OLD API
                // gatt.writeDescriptor(it, enableNotificationByte) // NEW API 33....
                gatt.writeDescriptor(it) // OLD API
                Log.d("AppServiceDisconnection", "Written to Descriptor: $enableNotificationByte.toString()")
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public fun requestDisconnection() {
        Log.d("AppServiceDisconnection", "Disconnection to Gatt requested")
        _bleState.value.connectionState.gatt?.disconnect()
    }

    /* This function is actually responsible to completely destroying the service
    * which is called from onConnectionChanged after BLE gatt connection is fully closed */
    public fun stopService(){
        Log.d("AppServiceDisconnection", "SERVICE DESTROYED!!")

        // ----------------------------------------------------------------------
        //                                NOTE
        // ----------------------------------------------------------------------
        // For some odd reason, destroying the service doesn't fully destroy all
        // the class attributes???? why i don't know -> maybe it takes time fully clear?
        // and reopening the app fast enough will cause trouble
        // ----------------------------------------------------------------------
        bluetoothGatt = null
        isConnecting = false
        isServiceDestructionRequired = false
        // ----------------------------------------------------------------------

        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
        super.onDestroy()
    }
}