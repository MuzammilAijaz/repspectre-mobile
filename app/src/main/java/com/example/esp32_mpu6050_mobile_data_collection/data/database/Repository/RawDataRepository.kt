package com.example.esp32_mpu6050_mobile_data_collection.data.database.Repository

import com.example.esp32_mpu6050_mobile_data_collection.data.database.DAO.RawDataDao
import com.example.esp32_mpu6050_mobile_data_collection.data.database.entity.RawDataEntity

class RawDataRepository(
    private val rawDataDao: RawDataDao
) : SessionRepository<RawDataEntity>(rawDataDao)
