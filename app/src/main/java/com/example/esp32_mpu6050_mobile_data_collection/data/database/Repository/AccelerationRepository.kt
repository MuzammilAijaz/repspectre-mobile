package com.example.esp32_mpu6050_mobile_data_collection.data.database.Repository

import com.example.esp32_mpu6050_mobile_data_collection.data.database.DAO.AccelerationDao
import com.example.esp32_mpu6050_mobile_data_collection.data.database.entity.AccelerationEntity

class AccelerationRepository(
    private val accelerationDao: AccelerationDao
) : SessionRepository<AccelerationEntity>(accelerationDao)
