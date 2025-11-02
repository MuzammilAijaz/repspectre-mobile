package com.example.esp32_mpu6050_mobile_data_collection.service

import android.Manifest
import android.app.ForegroundServiceStartNotAllowedException
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.PermissionChecker
import com.example.esp32_mpu6050_mobile_data_collection.R
import com.example.esp32_mpu6050_mobile_data_collection.raylib.updateNativeOrientation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

// =====================================================================================
// |                                   Service
// -------------------------------------------------------------------------------------
// | Holds the data to be sent to the native side using JNI, periodically
// |
// | Automatically upgrades itself to foreground service on its creation.
// |
// =====================================================================================

class AppService: Service() {

    private var isServiceEnabled: Boolean = false
    // --------------------------------------------------------------
    //                              Data
    // --------------------------------------------------------------
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // ---- Binder ----
    private val binder = LocalBinder()
    inner class LocalBinder : Binder() {
        fun getService(): AppService = this@AppService
    }

    data class MyData(
        @Volatile var x: Float = 1.0f,
        @Volatile var y: Float = 1.0f,
        @Volatile var z: Float = 1.0f
    )

    public lateinit var myData: MyData

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

        // ------------------ 2. Call startForeground with permission ------------------
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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isServiceEnabled = true
        startForeground() // upgrade the service to foreground service
        serviceScope.launch {
            while (isServiceEnabled) {
                try {
                    // Randomly Update Data; meant to resemble orientation changes
                    myData.x = Random.nextFloat() * 360f
                    myData.y = Random.nextFloat() * 360f
                    myData.z = Random.nextFloat() * 360f

                    val start = System.nanoTime()

                    updateNativeOrientation(myData.x, myData.y, myData.z);

                    val end = System.nanoTime()
                    val durationNs = end - start
                    Log.d("JNI_TEST", "updateOrientation took ${durationNs}ns")

                }
                catch (t: Throwable) {
                    Log.d("AppService", "EXCPEPTION", t)
                }
                delay(10)
            }
        }

        return START_STICKY // Restarts service if service gets killed
    }

    override fun onBind(p0: Intent?): IBinder? {
        return binder
    }

    override fun onDestroy() {
        Log.d("AppService", "App Service DESTROYED!!")
        isServiceEnabled = false
        serviceScope.cancel()
    }

    // --------------------------------------------------------------
    //                        Public Functions
    // --------------------------------------------------------------
}