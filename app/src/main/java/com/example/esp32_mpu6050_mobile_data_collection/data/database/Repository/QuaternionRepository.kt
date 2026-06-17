package com.example.esp32_mpu6050_mobile_data_collection.data.database.Repository

import com.example.esp32_mpu6050_mobile_data_collection.data.database.DAO.QuaternionDao
import com.example.esp32_mpu6050_mobile_data_collection.data.database.entity.QuaternionEntity

class QuaternionRepository(
    private val quaternionDao: QuaternionDao
) : SessionRepository<QuaternionEntity>(quaternionDao)
