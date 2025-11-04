package com.example.esp32_mpu6050_mobile_data_collection.data.entity

data class SensorEntities(
    val accelerationEntity: AccelerationEntity,
    val quaternionEntity: QuaternionEntity,
)