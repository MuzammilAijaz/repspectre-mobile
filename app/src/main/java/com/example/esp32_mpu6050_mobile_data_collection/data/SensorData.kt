package com.example.esp32_mpu6050_mobile_data_collection.data

sealed interface SensorData {
    /**
     * Complete packet received from the MCU.
     *
     * This is the canonical representation of a new BLE sample.
     * The values undergo minimal processing/conversion at the MCU and BLE layers.
     *
     * Accelerometer and gyroscope values are preserved as int16_t values from
     * the MPU6050 and are therefore represented as Short on Android.
     *
     * Quaternion values are extracted from the DMP FIFO as int16_t fixed-point
     * values and converted to normalized floats by dividing by 16384.0f in
     * the MPU6050 library.
     */
    data class FullIMURaw(
        val ax: Short,
        val ay: Short,
        val az: Short,

        val gx: Short,
        val gy: Short,
        val gz: Short,

        val qx: Float,
        val qy: Float,
        val qz: Float,
        val qw: Float,

        val timestampUs: Long,
    ) : SensorData

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
