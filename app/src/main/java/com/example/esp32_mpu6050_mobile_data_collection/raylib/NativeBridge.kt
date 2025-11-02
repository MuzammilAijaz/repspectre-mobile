package com.example.esp32_mpu6050_mobile_data_collection.raylib

object NativeBridge {
    init {
        System.loadLibrary("raymob")
    }
    external fun updateOrientation(x: Float, y: Float, z: Float)
}

/**
 * Pass the shared repository data to the native layer.
 *
 * This function is called whenever there will be an update
 * of the values, managed by a thread. */
fun updateNativeOrientation(x: Float, y: Float, z: Float) {
    NativeBridge.updateOrientation(x, y, z)
}
