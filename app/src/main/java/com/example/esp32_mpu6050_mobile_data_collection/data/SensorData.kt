package com.example.esp32_mpu6050_mobile_data_collection.data

sealed interface SensorData {
    data class Acceleration(
        val x: Float,
        val y: Float,
        val z: Float,
    ) : SensorData

    data class Quaternion(
        val x: Float,
        val y: Float,
        val z: Float,
        val w: Float,
    ) : SensorData

    data class Raw(
        val ax: Float,
        val ay: Float,
        val az: Float,

        val gx: Float,
        val gy: Float,
        val gz: Float,
    ) : SensorData
}