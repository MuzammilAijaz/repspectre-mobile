package com.example.esp32_mpu6050_mobile_data_collection.data.database.Repository

import com.example.esp32_mpu6050_mobile_data_collection.data.database.DAO.FullIMURawDao
import com.example.esp32_mpu6050_mobile_data_collection.data.database.entity.FullIMURawEntity

class FullIMURawRepository(
    private val fullIMURawDao: FullIMURawDao
) : SessionRepository<FullIMURawEntity>(fullIMURawDao)
