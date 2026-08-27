package com.example.esp32_mpu6050_mobile_data_collection.data

const val MPU6050_GYRO_FS_250_LSB_PER_DPS = 131f   // ±250 °/s
const val MPU6050_ACCEL_FS_2_LSB_PER_G = 16384f    // ±2 g

// WARN: ensure these are the proper values set in firmware.
// @See mpuInitialize() or getFullScaleAccelRange/getFullScaleGyroRange if using i2cdev lib
const val GYRO_LSB_PER_DPS = MPU6050_GYRO_FS_250_LSB_PER_DPS
const val ACCEL_LSB_PER_G = MPU6050_ACCEL_FS_2_LSB_PER_G

sealed interface SensorData {
    /**
     * Complete packet received from the MCU.
     *
     * This is the canonical representation of a new BLE sample.
     * The values undergo minimal processing/conversion at the MCU and BLE layers.
     *
     * Accelerometer and gyroscope values are assumed to have converted to float
     * using the following calculations:
     *      accel = (int16_t) accelShort / [MPU6050_ACCEL_FS_2_LSB_PER_G]
     *      gyro = (int16_t) gyroShort / [MPU6050_GYRO_FS_250_LSB_PER_DPS]
     *
     * @see MPU6050_ACCEL_FS_2 & MPU6050_GYRO_FS_250 full-scale range values in firmware code,
     * which is the most sensitive settings for mpu6050 (+/- 2g and +/- 250 degrees/sec)
     *
     * Quaternion values are extracted from the DMP FIFO as int16_t fixed-point
     * values and converted to normalized floats by dividing by 16384.0f in
     * the MPU6050 library.
     */
    data class FullIMURaw(
        val ax: Float,
        val ay: Float,
        val az: Float,

        val gx: Float,
        val gy: Float,
        val gz: Float,

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
